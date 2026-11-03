package com.aser.asertrading.procurement.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.inventory.service.WeightedAverageCostService;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.procurement.domain.*;
import com.aser.asertrading.procurement.repository.GoodsReceiptRepository;
import com.aser.asertrading.procurement.repository.PurchaseOrderRepository;
import com.aser.asertrading.procurement.repository.SupplierRepository;
import com.aser.asertrading.procurement.web.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcurementService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WeightedAverageCostService costService;
    private final AuditService auditService;

    // Supplier operations
    @Transactional(readOnly = true)
    public List<SupplierDto> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(SupplierDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public Page<SupplierDto> getSuppliers(Pageable pageable) {
        return supplierRepository.findAll(pageable).map(SupplierDto::fromEntity);
    }

    @Transactional
    public SupplierDto createSupplier(CreateSupplierRequest request, String actor) {
        String code = request.code().trim().toUpperCase();
        if (supplierRepository.existsByCode(code)) {
            throw new BadRequestException("Supplier code '" + code + "' already exists");
        }

        Supplier supplier = Supplier.builder()
                .name(request.name().trim())
                .code(code)
                .contactPerson(request.contactPerson())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .city(request.city())
                .taxId(request.taxId())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        supplier = supplierRepository.save(supplier);
        auditService.log(actor, "CREATE_SUPPLIER", "Supplier", supplier.getId().toString(), "Created supplier " + supplier.getName(), null);
        return SupplierDto.fromEntity(supplier);
    }

    // Purchase Order operations
    @Transactional
    public PurchaseOrderDto createPurchaseOrder(CreatePORequest request, String actor) {
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.warehouseId()));

        String poNumber = "PO-" + System.currentTimeMillis() % 10000000;

        PurchaseOrder po = PurchaseOrder.builder()
                .poNumber(poNumber)
                .supplierId(supplier.getId())
                .warehouseId(warehouse.getId())
                .status(POStatus.DRAFT)
                .notes(request.notes())
                .createdBy(actor)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (POItemRequest itemReq : request.items()) {
            if (!productRepository.existsById(itemReq.productId())) {
                throw new ResourceNotFoundException("Product not found with id: " + itemReq.productId());
            }

            BigDecimal itemTotal = itemReq.quantity().multiply(itemReq.unitCost()).setScale(2, RoundingMode.HALF_UP);
            totalAmount = totalAmount.add(itemTotal);

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .purchaseOrder(po)
                    .productId(itemReq.productId())
                    .quantity(itemReq.quantity())
                    .unitCost(itemReq.unitCost())
                    .totalCost(itemTotal)
                    .receivedQuantity(BigDecimal.ZERO)
                    .build();

            po.getItems().add(item);
        }

        po.setTotalAmount(totalAmount);
        po = purchaseOrderRepository.save(po);

        auditService.log(actor, "CREATE_PURCHASE_ORDER", "PurchaseOrder", po.getId().toString(),
                "Created PO " + po.getPoNumber() + " for supplier " + supplier.getName() + " totaling " + po.getTotalAmount(), null);

        return mapToDto(po);
    }

    @Transactional
    public PurchaseOrderDto approvePurchaseOrder(Long id, String approver) {
        PurchaseOrder po = getPOEntity(id);
        if (po.getStatus() != POStatus.DRAFT && po.getStatus() != POStatus.SUBMITTED) {
            throw new BadRequestException("Purchase order cannot be approved from status: " + po.getStatus());
        }

        po.setStatus(POStatus.APPROVED);
        po.setApprovedBy(approver);
        po = purchaseOrderRepository.save(po);

        auditService.log(approver, "APPROVE_PURCHASE_ORDER", "PurchaseOrder", po.getId().toString(),
                "Approved purchase order " + po.getPoNumber(), null);

        return mapToDto(po);
    }

    @Transactional
    public GoodsReceiptDto recordGoodsReceipt(Long poId, GoodsReceiptRequest request, String actor) {
        PurchaseOrder po = getPOEntity(poId);
        if (po.getStatus() != POStatus.APPROVED && po.getStatus() != POStatus.PARTIALLY_RECEIVED) {
            throw new BadRequestException("Cannot receive goods for purchase order with status: " + po.getStatus());
        }

        Warehouse warehouse = warehouseRepository.findById(po.getWarehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination warehouse not found with id: " + po.getWarehouseId()));

        String grnNumber = "GRN-" + System.currentTimeMillis() % 10000000;

        GoodsReceipt grn = GoodsReceipt.builder()
                .grnNumber(grnNumber)
                .purchaseOrderId(po.getId())
                .warehouseId(po.getWarehouseId())
                .receivedBy(actor)
                .deliveryNoteNumber(request.deliveryNoteNumber())
                .notes(request.notes())
                .build();

        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        for (GoodsReceiptItemRequest itemReq : request.items()) {
            PurchaseOrderItem poItem = poItemMap.get(itemReq.purchaseOrderItemId());
            if (poItem == null) {
                throw new BadRequestException("Purchase order item ID " + itemReq.purchaseOrderItemId() + " does not belong to PO " + po.getPoNumber());
            }

            BigDecimal receivedQty = itemReq.quantityReceived();
            BigDecimal unitCost = itemReq.unitCost() != null ? itemReq.unitCost() : poItem.getUnitCost();

            // Update PO Item received quantity
            poItem.setReceivedQuantity(poItem.getReceivedQuantity().add(receivedQty));

            GoodsReceiptItem grnItem = GoodsReceiptItem.builder()
                    .goodsReceipt(grn)
                    .productId(poItem.getProductId())
                    .quantityReceived(receivedQty)
                    .unitCost(unitCost)
                    .build();
            grn.getItems().add(grnItem);

            // Update Stock Balance and recalculate weighted average cost
            StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(poItem.getProductId(), warehouse.getId())
                    .orElseGet(() -> StockBalance.builder()
                            .productId(poItem.getProductId())
                            .warehouseId(warehouse.getId())
                            .onHandQuantity(BigDecimal.ZERO)
                            .reservedQuantity(BigDecimal.ZERO)
                            .weightedAverageCost(unitCost)
                            .build());

            BigDecimal newAvgCost = costService.calculateNewAverageCost(
                    balance.getOnHandQuantity(), balance.getWeightedAverageCost(),
                    receivedQty, unitCost
            );

            balance.setOnHandQuantity(balance.getOnHandQuantity().add(receivedQty));
            balance.setWeightedAverageCost(newAvgCost);
            stockBalanceRepository.save(balance);

            // Record append-only stock movement
            BigDecimal itemTotalCost = receivedQty.multiply(unitCost).setScale(4, RoundingMode.HALF_UP);
            StockMovement movement = StockMovement.builder()
                    .productId(poItem.getProductId())
                    .warehouseId(warehouse.getId())
                    .quantityDelta(receivedQty)
                    .movementType(StockMovementType.RECEIPT)
                    .referenceType(ReferenceType.PURCHASE_ORDER)
                    .referenceId(po.getPoNumber() + " (" + grn.getGrnNumber() + ")")
                    .unitCost(unitCost)
                    .totalCost(itemTotalCost)
                    .actor(actor)
                    .notes("Supplier delivery against PO " + po.getPoNumber())
                    .build();
            stockMovementRepository.save(movement);
        }

        grn = goodsReceiptRepository.save(grn);

        // Check if PO is fully received or partially received
        boolean allFullyReceived = po.getItems().stream()
                .allMatch(item -> item.getReceivedQuantity().compareTo(item.getQuantity()) >= 0);

        po.setStatus(allFullyReceived ? POStatus.RECEIVED : POStatus.PARTIALLY_RECEIVED);
        purchaseOrderRepository.save(po);

        auditService.log(actor, "GOODS_RECEIPT", "GoodsReceipt", grn.getId().toString(),
                "Received GRN " + grn.getGrnNumber() + " against PO " + po.getPoNumber(), null);

        Map<Long, Product> productMap = productRepository.findAllById(
                grn.getItems().stream().map(GoodsReceiptItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<GoodsReceiptItemDto> itemDtos = grn.getItems().stream().map(i -> {
            Product p = productMap.get(i.getProductId());
            return GoodsReceiptItemDto.of(i, p != null ? p.getSku() : "UNKNOWN", p != null ? p.getName() : "UNKNOWN");
        }).toList();

        return GoodsReceiptDto.of(grn, warehouse.getName(), itemDtos);
    }

    @Transactional(readOnly = true)
    public Page<PurchaseOrderDto> getPurchaseOrders(POStatus status, Pageable pageable) {
        Page<PurchaseOrder> page = status != null
                ? purchaseOrderRepository.findByStatus(status, pageable)
                : purchaseOrderRepository.findAll(pageable);

        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDto getPurchaseOrderById(Long id) {
        return mapToDto(getPOEntity(id));
    }

    private PurchaseOrder getPOEntity(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
    }

    private PurchaseOrderDto mapToDto(PurchaseOrder po) {
        Supplier supplier = supplierRepository.findById(po.getSupplierId()).orElse(null);
        Warehouse warehouse = warehouseRepository.findById(po.getWarehouseId()).orElse(null);

        Map<Long, Product> productMap = productRepository.findAllById(
                po.getItems().stream().map(PurchaseOrderItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<POItemDto> items = po.getItems().stream().map(i -> {
            Product p = productMap.get(i.getProductId());
            return POItemDto.of(i, p != null ? p.getSku() : "UNKNOWN", p != null ? p.getName() : "UNKNOWN");
        }).toList();

        return PurchaseOrderDto.of(
                po,
                supplier != null ? supplier.getName() : "UNKNOWN",
                warehouse != null ? warehouse.getName() : "UNKNOWN",
                items
        );
    }
}

