package com.aser.asertrading.transfer.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.InsufficientStockException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.repository.UserRepository;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.inventory.service.WeightedAverageCostService;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.transfer.domain.*;
import com.aser.asertrading.transfer.repository.StockTransferRepository;
import com.aser.asertrading.transfer.repository.TransferDiscrepancyRepository;
import com.aser.asertrading.transfer.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferWorkflowService {

    private final StockTransferRepository transferRepository;
    private final TransferDiscrepancyRepository discrepancyRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WeightedAverageCostService costService;
    private final AuditService auditService;
    private final UserRepository userRepository;

    @Transactional
    public StockTransferDto createTransfer(CreateTransferRequest request, String actor) {
        if (request.sourceWarehouseId().equals(request.destinationWarehouseId())) {
            throw new BadRequestException("Source and destination warehouses must be different");
        }

        Warehouse sourceWh = warehouseRepository.findById(request.sourceWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Source warehouse not found with id: " + request.sourceWarehouseId()));

        Warehouse destWh = warehouseRepository.findById(request.destinationWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found with id: " + request.destinationWarehouseId()));

        String transferNumber = "TRF-" + System.currentTimeMillis() % 10000000;

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(transferNumber)
                .sourceWarehouseId(sourceWh.getId())
                .destinationWarehouseId(destWh.getId())
                .status(TransferStatus.DRAFT)
                .requestedBy(actor)
                .notes(request.notes())
                .build();

        for (TransferItemRequest itemReq : request.items()) {
            if (!productRepository.existsById(itemReq.productId())) {
                throw new ResourceNotFoundException("Product not found with id: " + itemReq.productId());
            }

            StockTransferItem item = StockTransferItem.builder()
                    .transfer(transfer)
                    .productId(itemReq.productId())
                    .requestedQuantity(itemReq.quantity())
                    .build();
            transfer.getItems().add(item);
        }

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType("CREATED")
                .actor(actor)
                .notes("Transfer request created in DRAFT")
                .build();
        transfer.getEvents().add(event);

        transfer = transferRepository.save(transfer);

        auditService.log(actor, "CREATE_TRANSFER", "StockTransfer", transfer.getId().toString(),
                "Created transfer " + transfer.getTransferNumber() + " from " + sourceWh.getName() + " to " + destWh.getName(), null);

        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferDto submitTransfer(Long transferId, String actor) {
        StockTransfer transfer = getTransferEntity(transferId);
        if (transfer.getStatus() != TransferStatus.DRAFT) {
            throw new BadRequestException("Only transfers in DRAFT status can be submitted for approval");
        }

        transfer.setStatus(TransferStatus.PENDING_APPROVAL);

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType("SUBMITTED")
                .actor(actor)
                .notes("Submitted for management approval")
                .build();
        transfer.getEvents().add(event);

        transfer = transferRepository.save(transfer);

        auditService.log(actor, "SUBMIT_TRANSFER", "StockTransfer", transfer.getId().toString(),
                "Submitted transfer " + transfer.getTransferNumber() + " for approval", null);

        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferDto approveTransfer(Long transferId, String approver) {
        StockTransfer transfer = getTransferEntity(transferId);
        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Transfer is not awaiting approval. Current status: " + transfer.getStatus());
        }

        // Separation of duties rule: Requester cannot approve their own transfer
        // Note: Administrators have system-wide override authority per SRS
        boolean isSystemAdmin = "admin".equalsIgnoreCase(approver) ||
                (userRepository.findByUsername(approver).map(u -> u.getRole() == Role.SYSTEM_ADMINISTRATOR).orElse(false));

        if (!isSystemAdmin && approver != null && approver.equalsIgnoreCase(transfer.getRequestedBy())) {
            throw new BadRequestException("Separation of duties violation: You cannot approve your own transfer request.");
        }

        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(approver);

        for (StockTransferItem item : transfer.getItems()) {
            item.setApprovedQuantity(item.getRequestedQuantity());
        }

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType("APPROVED")
                .actor(approver)
                .notes("Approved by " + approver)
                .build();
        transfer.getEvents().add(event);

        transfer = transferRepository.save(transfer);

        auditService.log(approver, "APPROVE_TRANSFER", "StockTransfer", transfer.getId().toString(),
                "Approved transfer " + transfer.getTransferNumber(), null);

        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferDto rejectTransfer(Long transferId, String reason, String actor) {
        StockTransfer transfer = getTransferEntity(transferId);
        if (transfer.getStatus() != TransferStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Transfer cannot be rejected in current status: " + transfer.getStatus());
        }

        transfer.setStatus(TransferStatus.REJECTED);
        transfer.setRejectionReason(reason);

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType("REJECTED")
                .actor(actor)
                .notes("Rejected: " + reason)
                .build();
        transfer.getEvents().add(event);

        transfer = transferRepository.save(transfer);

        auditService.log(actor, "REJECT_TRANSFER", "StockTransfer", transfer.getId().toString(),
                "Rejected transfer " + transfer.getTransferNumber() + ": " + reason, null);

        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferDto dispatchTransfer(Long transferId, String actor) {
        StockTransfer transfer = getTransferEntity(transferId);
        if (transfer.getStatus() != TransferStatus.APPROVED) {
            throw new BadRequestException("Only APPROVED transfers can be dispatched. Current status: " + transfer.getStatus());
        }

        // Validate stock availability and deduct from source warehouse atomically
        for (StockTransferItem item : transfer.getItems()) {
            BigDecimal dispatchQty = item.getApprovedQuantity() != null ? item.getApprovedQuantity() : item.getRequestedQuantity();

            StockBalance sourceBalance = stockBalanceRepository.findByProductIdAndWarehouseId(item.getProductId(), transfer.getSourceWarehouseId())
                    .orElseThrow(() -> new InsufficientStockException("No stock balance found for product ID " + item.getProductId() + " in source warehouse"));

            if (sourceBalance.getAvailableQuantity().compareTo(dispatchQty) < 0) {
                throw new InsufficientStockException(String.format(
                        "Cannot dispatch transfer: Available stock (%s) is less than approved quantity (%s) for product ID %d",
                        sourceBalance.getAvailableQuantity(), dispatchQty, item.getProductId()
                ));
            }

            sourceBalance.setOnHandQuantity(sourceBalance.getOnHandQuantity().subtract(dispatchQty));
            stockBalanceRepository.save(sourceBalance);

            item.setDispatchedQuantity(dispatchQty);

            // Record append-only TRANSFER_OUT movement
            BigDecimal unitCost = sourceBalance.getWeightedAverageCost();
            BigDecimal totalCost = unitCost != null ? unitCost.multiply(dispatchQty) : BigDecimal.ZERO;

            StockMovement movement = StockMovement.builder()
                    .productId(item.getProductId())
                    .warehouseId(transfer.getSourceWarehouseId())
                    .quantityDelta(dispatchQty.negate())
                    .movementType(StockMovementType.TRANSFER_OUT)
                    .referenceType(ReferenceType.TRANSFER)
                    .referenceId(transfer.getTransferNumber())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .actor(actor)
                    .notes("Dispatched for transfer " + transfer.getTransferNumber())
                    .build();

            stockMovementRepository.save(movement);
        }

        transfer.setStatus(TransferStatus.IN_TRANSIT);
        transfer.setDispatchedBy(actor);

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType("DISPATCHED")
                .actor(actor)
                .notes("Goods dispatched; in transit to destination")
                .build();
        transfer.getEvents().add(event);

        transfer = transferRepository.save(transfer);

        auditService.log(actor, "DISPATCH_TRANSFER", "StockTransfer", transfer.getId().toString(),
                "Dispatched transfer " + transfer.getTransferNumber(), null);

        return mapToDto(transfer);
    }

    @Transactional
    public StockTransferDto receiveTransfer(Long transferId, TransferReceiveRequest request, String actor) {
        StockTransfer transfer = getTransferEntity(transferId);
        if (transfer.getStatus() != TransferStatus.IN_TRANSIT) {
            throw new BadRequestException("Transfer cannot be received. Current status: " + transfer.getStatus());
        }

        Map<Long, StockTransferItem> itemMap = transfer.getItems().stream()
                .collect(Collectors.toMap(StockTransferItem::getId, item -> item));

        boolean hasDiscrepancy = false;

        for (TransferReceiveItemRequest recItemReq : request.items()) {
            StockTransferItem item = itemMap.get(recItemReq.transferItemId());
            if (item == null) {
                throw new BadRequestException("Item ID " + recItemReq.transferItemId() + " does not belong to this transfer");
            }

            BigDecimal receivedQty = recItemReq.receivedQuantity();
            BigDecimal expectedQty = item.getDispatchedQuantity() != null ? item.getDispatchedQuantity() : BigDecimal.ZERO;
            item.setReceivedQuantity(receivedQty);

            // Fetch source cost from the source stock movement
            BigDecimal transferCost = stockMovementRepository.findByReferenceTypeAndReferenceId(ReferenceType.TRANSFER, transfer.getTransferNumber())
                    .stream()
                    .filter(m -> m.getProductId().equals(item.getProductId()) && m.getMovementType() == StockMovementType.TRANSFER_OUT)
                    .findFirst()
                    .map(StockMovement::getUnitCost)
                    .orElse(BigDecimal.ZERO);

            // Post stock increase at destination
            StockBalance destBalance = stockBalanceRepository.findByProductIdAndWarehouseId(item.getProductId(), transfer.getDestinationWarehouseId())
                    .orElseGet(() -> StockBalance.builder()
                            .productId(item.getProductId())
                            .warehouseId(transfer.getDestinationWarehouseId())
                            .onHandQuantity(BigDecimal.ZERO)
                            .reservedQuantity(BigDecimal.ZERO)
                            .weightedAverageCost(transferCost)
                            .build());

            BigDecimal newAvgCost = costService.calculateNewAverageCost(
                    destBalance.getOnHandQuantity(), destBalance.getWeightedAverageCost(),
                    receivedQty, transferCost
            );

            destBalance.setOnHandQuantity(destBalance.getOnHandQuantity().add(receivedQty));
            destBalance.setWeightedAverageCost(newAvgCost);
            stockBalanceRepository.save(destBalance);

            // Record append-only TRANSFER_IN movement
            BigDecimal totalCost = receivedQty.multiply(transferCost).setScale(4, java.math.RoundingMode.HALF_UP);
            StockMovement inMovement = StockMovement.builder()
                    .productId(item.getProductId())
                    .warehouseId(transfer.getDestinationWarehouseId())
                    .quantityDelta(receivedQty)
                    .movementType(StockMovementType.TRANSFER_IN)
                    .referenceType(ReferenceType.TRANSFER)
                    .referenceId(transfer.getTransferNumber())
                    .unitCost(transferCost)
                    .totalCost(totalCost)
                    .actor(actor)
                    .notes("Received from transfer " + transfer.getTransferNumber())
                    .build();
            stockMovementRepository.save(inMovement);

            // Check discrepancy
            if (receivedQty.compareTo(expectedQty) != 0) {
                hasDiscrepancy = true;
                DiscrepancyType discType = receivedQty.compareTo(expectedQty) < 0
                        ? DiscrepancyType.SHORTAGE
                        : DiscrepancyType.EXCESS;

                TransferDiscrepancy disc = TransferDiscrepancy.builder()
                        .transferId(transfer.getId())
                        .productId(item.getProductId())
                        .expectedQuantity(expectedQty)
                        .actualQuantity(receivedQty)
                        .discrepancyType(discType)
                        .reportedBy(actor)
                        .resolutionNotes("Discrepancy recorded during destination receipt")
                        .build();

                discrepancyRepository.save(disc);
            }
        }

        transfer.setReceivedBy(actor);
        if (hasDiscrepancy) {
            transfer.setStatus(TransferStatus.DISCREPANCY_REVIEW);
        } else {
            transfer.setStatus(TransferStatus.RECEIVED);
        }

        TransferEvent event = TransferEvent.builder()
                .transfer(transfer)
                .eventType(hasDiscrepancy ? "RECEIVED_WITH_DISCREPANCY" : "RECEIVED")
                .actor(actor)
                .notes(request.notes() != null ? request.notes() : "Transfer goods received at destination warehouse")
                .build();
        transfer.getEvents().add(event);

        StockTransfer savedTransfer = transferRepository.save(transfer);

        auditService.log(actor, "RECEIVE_TRANSFER", "StockTransfer", savedTransfer.getId().toString(),
                "Received transfer " + savedTransfer.getTransferNumber() + " (Discrepancy: " + hasDiscrepancy + ")", null);

        return mapToDto(savedTransfer);
    }

    @Transactional
    public TransferDiscrepancyDto reportDiscrepancy(Long transferId, ReportDiscrepancyRequest request, String actor) {
        StockTransfer transfer = getTransferEntity(transferId);

        TransferDiscrepancy disc = TransferDiscrepancy.builder()
                .transferId(transfer.getId())
                .productId(request.productId())
                .expectedQuantity(request.expectedQuantity())
                .actualQuantity(request.actualQuantity())
                .discrepancyType(request.discrepancyType())
                .reportedBy(actor)
                .resolutionNotes(request.resolutionNotes())
                .build();

        disc = discrepancyRepository.save(disc);

        transfer.setStatus(TransferStatus.DISCREPANCY_REVIEW);
        transferRepository.save(transfer);

        Product product = productRepository.findById(request.productId()).orElse(null);
        String sku = product != null ? product.getSku() : "UNKNOWN";
        String name = product != null ? product.getName() : "UNKNOWN";

        auditService.log(actor, "REPORT_TRANSFER_DISCREPANCY", "TransferDiscrepancy", disc.getId().toString(),
                "Reported discrepancy for transfer " + transfer.getTransferNumber() + " on SKU " + sku, null);

        return TransferDiscrepancyDto.of(disc, sku, name);
    }

    @Transactional(readOnly = true)
    public Page<StockTransferDto> getTransfers(TransferStatus status, Pageable pageable) {
        Page<StockTransfer> transfers = status != null
                ? transferRepository.findByStatus(status, pageable)
                : transferRepository.findAll(pageable);

        return transfers.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public StockTransferDto getTransferById(Long id) {
        return mapToDto(getTransferEntity(id));
    }

    @Transactional(readOnly = true)
    public List<TransferEventDto> getTransferEvents(Long transferId) {
        StockTransfer transfer = getTransferEntity(transferId);
        return transfer.getEvents().stream().map(TransferEventDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferDiscrepancyDto> getDiscrepancies(Long transferId) {
        List<TransferDiscrepancy> list = discrepancyRepository.findByTransferId(transferId);
        Map<Long, Product> productMap = productRepository.findAllById(
                list.stream().map(TransferDiscrepancy::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        return list.stream().map(d -> {
            Product p = productMap.get(d.getProductId());
            return TransferDiscrepancyDto.of(d, p != null ? p.getSku() : "UNKNOWN", p != null ? p.getName() : "UNKNOWN");
        }).toList();
    }

    private StockTransfer getTransferEntity(Long id) {
        return transferRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stock transfer not found with id: " + id));
    }

    private StockTransferDto mapToDto(StockTransfer transfer) {
        Warehouse sourceWh = warehouseRepository.findById(transfer.getSourceWarehouseId()).orElse(null);
        Warehouse destWh = warehouseRepository.findById(transfer.getDestinationWarehouseId()).orElse(null);

        Map<Long, Product> productMap = productRepository.findAllById(
                transfer.getItems().stream().map(StockTransferItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<TransferItemDto> itemDtos = transfer.getItems().stream().map(item -> {
            Product p = productMap.get(item.getProductId());
            return TransferItemDto.of(item, p != null ? p.getSku() : "UNKNOWN", p != null ? p.getName() : "UNKNOWN");
        }).toList();

        List<TransferEventDto> eventDtos = transfer.getEvents().stream()
                .map(TransferEventDto::fromEntity)
                .toList();

        return StockTransferDto.of(
                transfer,
                sourceWh != null ? sourceWh.getName() : "UNKNOWN",
                destWh != null ? destWh.getName() : "UNKNOWN",
                itemDtos,
                eventDtos
        );
    }
}

