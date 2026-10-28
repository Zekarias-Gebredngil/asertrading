package com.aser.asertrading.inventory.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.InsufficientStockException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.domain.*;
import com.aser.asertrading.inventory.repository.*;
import com.aser.asertrading.inventory.web.dto.*;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final InventoryAdjustmentRepository adjustmentRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final WeightedAverageCostService costService;
    private final AuditService auditService;

    @Transactional
    public StockBalanceDto recordReceipt(StockReceiptRequest request, String actor) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.warehouseId()));

        StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockBalance.builder()
                        .productId(product.getId())
                        .warehouseId(warehouse.getId())
                        .onHandQuantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .weightedAverageCost(request.unitCost())
                        .build());

        BigDecimal existingQty = balance.getOnHandQuantity();
        BigDecimal existingCost = balance.getWeightedAverageCost();
        BigDecimal receivedQty = request.quantity();
        BigDecimal receivedCost = request.unitCost();

        // Calculate weighted average cost per Section 7
        BigDecimal newAvgCost = costService.calculateNewAverageCost(existingQty, existingCost, receivedQty, receivedCost);

        balance.setOnHandQuantity(existingQty.add(receivedQty));
        balance.setWeightedAverageCost(newAvgCost);
        balance = stockBalanceRepository.save(balance);

        BigDecimal totalCost = receivedQty.multiply(receivedCost).setScale(4, java.math.RoundingMode.HALF_UP);

        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantityDelta(receivedQty)
                .movementType(StockMovementType.RECEIPT)
                .referenceType(request.referenceType() != null ? request.referenceType() : ReferenceType.MANUAL)
                .referenceId(request.referenceId())
                .unitCost(receivedCost)
                .totalCost(totalCost)
                .actor(actor)
                .notes(request.notes())
                .build();

        stockMovementRepository.save(movement);

        auditService.log(actor, "STOCK_RECEIPT", "StockBalance", balance.getId().toString(),
                String.format("Received %s units of SKU %s at cost %s. New avg cost: %s",
                        receivedQty, product.getSku(), receivedCost, newAvgCost), null);

        String catName = product.getCategory() != null ? product.getCategory().getName() : "General";
        String unitSymbol = product.getBaseUnit() != null ? product.getBaseUnit().getSymbol() : "pcs";
        Long branchId = warehouse.getBranch() != null ? warehouse.getBranch().getId() : null;
        String branchName = warehouse.getBranch() != null ? warehouse.getBranch().getName() : "Main Branch";
        return StockBalanceDto.of(balance, product.getSku(), product.getName(), catName, warehouse.getName(), branchId, branchName, unitSymbol, product.getReorderThreshold());
    }

    @Transactional
    public StockBalanceDto recordIssue(StockIssueRequest request, String actor) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.warehouseId()));

        StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseThrow(() -> new InsufficientStockException("No stock balance found for product " + product.getSku() + " in warehouse " + warehouse.getName()));

        BigDecimal requestedQty = request.quantity();
        if (balance.getAvailableQuantity().compareTo(requestedQty) < 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for SKU %s in warehouse %s. Available: %s, Requested: %s",
                    product.getSku(), warehouse.getName(), balance.getAvailableQuantity(), requestedQty
            ));
        }

        balance.setOnHandQuantity(balance.getOnHandQuantity().subtract(requestedQty));
        balance = stockBalanceRepository.save(balance);

        BigDecimal currentCost = balance.getWeightedAverageCost();
        BigDecimal totalCost = currentCost != null ? currentCost.multiply(requestedQty) : BigDecimal.ZERO;

        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantityDelta(requestedQty.negate())
                .movementType(StockMovementType.ISSUE)
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .unitCost(currentCost)
                .totalCost(totalCost)
                .actor(actor)
                .notes(request.notes())
                .build();

        stockMovementRepository.save(movement);

        auditService.log(actor, "STOCK_ISSUE", "StockBalance", balance.getId().toString(),
                String.format("Issued %s units of SKU %s from %s for %s (%s)",
                        requestedQty, product.getSku(), warehouse.getName(), request.referenceType(), request.referenceId()), null);

        String catName = product.getCategory() != null ? product.getCategory().getName() : "General";
        String unitSymbol = product.getBaseUnit() != null ? product.getBaseUnit().getSymbol() : "pcs";
        Long branchId = warehouse.getBranch() != null ? warehouse.getBranch().getId() : null;
        String branchName = warehouse.getBranch() != null ? warehouse.getBranch().getName() : "Main Branch";
        return StockBalanceDto.of(balance, product.getSku(), product.getName(), catName, warehouse.getName(), branchId, branchName, unitSymbol, product.getReorderThreshold());
    }

    @Transactional
    public StockBalanceDto recordAdjustment(InventoryAdjustmentRequest request, String actor) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.productId()));

        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + request.warehouseId()));

        StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> StockBalance.builder()
                        .productId(product.getId())
                        .warehouseId(warehouse.getId())
                        .onHandQuantity(BigDecimal.ZERO)
                        .reservedQuantity(BigDecimal.ZERO)
                        .weightedAverageCost(product.getPurchaseCost())
                        .build());

        BigDecimal delta = request.quantityDelta();
        BigDecimal newOnHand = balance.getOnHandQuantity().add(delta);
        if (newOnHand.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Adjustment would cause negative on-hand balance (" + newOnHand + ")");
        }

        balance.setOnHandQuantity(newOnHand);
        balance = stockBalanceRepository.save(balance);

        InventoryAdjustment adjustment = InventoryAdjustment.builder()
                .warehouseId(warehouse.getId())
                .productId(product.getId())
                .quantityDelta(delta)
                .reasonCode(request.reasonCode().trim().toUpperCase())
                .approvedBy(actor)
                .notes(request.notes())
                .build();
        adjustmentRepository.save(adjustment);

        BigDecimal currentCost = balance.getWeightedAverageCost();
        BigDecimal totalCost = currentCost != null ? currentCost.multiply(delta.abs()) : BigDecimal.ZERO;

        StockMovement movement = StockMovement.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .quantityDelta(delta)
                .movementType(StockMovementType.ADJUSTMENT)
                .referenceType(ReferenceType.ADJUSTMENT)
                .referenceId(adjustment.getId().toString())
                .unitCost(currentCost)
                .totalCost(totalCost)
                .actor(actor)
                .notes("Reason: " + request.reasonCode() + ". " + (request.notes() != null ? request.notes() : ""))
                .build();

        stockMovementRepository.save(movement);

        auditService.log(actor, "STOCK_ADJUSTMENT", "InventoryAdjustment", adjustment.getId().toString(),
                String.format("Adjusted SKU %s by delta %s in warehouse %s. Reason: %s",
                        product.getSku(), delta, warehouse.getName(), request.reasonCode()), null);

        String catName = product.getCategory() != null ? product.getCategory().getName() : "General";
        String unitSymbol = product.getBaseUnit() != null ? product.getBaseUnit().getSymbol() : "pcs";
        Long branchId = warehouse.getBranch() != null ? warehouse.getBranch().getId() : null;
        String branchName = warehouse.getBranch() != null ? warehouse.getBranch().getName() : "Main Branch";
        return StockBalanceDto.of(balance, product.getSku(), product.getName(), catName, warehouse.getName(), branchId, branchName, unitSymbol, product.getReorderThreshold());
    }

    @Transactional(readOnly = true)
    public Page<StockBalanceDto> getBalances(Long warehouseId, Pageable pageable) {
        Page<StockBalance> balances = warehouseId != null
                ? stockBalanceRepository.findByWarehouseId(warehouseId, pageable)
                : stockBalanceRepository.findAll(pageable);

        Map<Long, Product> productMap = productRepository.findAllById(
                balances.getContent().stream().map(StockBalance::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAllById(
                balances.getContent().stream().map(StockBalance::getWarehouseId).toList()
        ).stream().collect(Collectors.toMap(Warehouse::getId, w -> w));

        return balances.map(b -> {
            Product p = productMap.get(b.getProductId());
            Warehouse w = warehouseMap.get(b.getWarehouseId());
            String sku = p != null ? p.getSku() : "UNKNOWN";
            String name = p != null ? p.getName() : "UNKNOWN";
            String cName = p != null && p.getCategory() != null ? p.getCategory().getName() : "General";
            String uSymbol = p != null && p.getBaseUnit() != null ? p.getBaseUnit().getSymbol() : "pcs";
            BigDecimal threshold = p != null ? p.getReorderThreshold() : BigDecimal.valueOf(10);
            String wName = w != null ? w.getName() : "UNKNOWN";
            Long bId = w != null && w.getBranch() != null ? w.getBranch().getId() : null;
            String bName = w != null && w.getBranch() != null ? w.getBranch().getName() : "Main Branch";
            return StockBalanceDto.of(b, sku, name, cName, wName, bId, bName, uSymbol, threshold);
        });
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> getMovements(Long productId, Long warehouseId, StockMovementType type, Pageable pageable) {
        Page<StockMovement> movements;
        if (productId != null) {
            movements = stockMovementRepository.findByProductId(productId, pageable);
        } else if (warehouseId != null) {
            movements = stockMovementRepository.findByWarehouseId(warehouseId, pageable);
        } else if (type != null) {
            movements = stockMovementRepository.findByMovementType(type, pageable);
        } else {
            movements = stockMovementRepository.findAll(pageable);
        }

        Map<Long, Product> productMap = productRepository.findAllById(
                movements.getContent().stream().map(StockMovement::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAllById(
                movements.getContent().stream().map(StockMovement::getWarehouseId).toList()
        ).stream().collect(Collectors.toMap(Warehouse::getId, w -> w));

        return movements.map(m -> {
            Product p = productMap.get(m.getProductId());
            Warehouse w = warehouseMap.get(m.getWarehouseId());
            String sku = p != null ? p.getSku() : "UNKNOWN";
            String name = p != null ? p.getName() : "UNKNOWN";
            String uSymbol = p != null && p.getBaseUnit() != null ? p.getBaseUnit().getSymbol() : "pcs";
            String wName = w != null ? w.getName() : "UNKNOWN";
            String bName = w != null && w.getBranch() != null ? w.getBranch().getName() : "Main Branch";
            return StockMovementDto.of(m, sku, name, wName, bName, uSymbol);
        });
    }

    @Transactional(readOnly = true)
    public List<LowStockAlertDto> getLowStockAlerts(Long warehouseId) {
        List<StockBalance> balances = warehouseId != null
                ? stockBalanceRepository.findByWarehouseId(warehouseId)
                : stockBalanceRepository.findAll();

        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, Warehouse> warehouseMap = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getId, w -> w));

        List<LowStockAlertDto> alerts = new ArrayList<>();
        for (StockBalance b : balances) {
            Product p = productMap.get(b.getProductId());
            if (p != null && p.getReorderThreshold() != null) {
                BigDecimal available = b.getAvailableQuantity();
                if (available.compareTo(p.getReorderThreshold()) <= 0) {
                    Warehouse w = warehouseMap.get(b.getWarehouseId());
                    BigDecimal shortage = p.getReorderThreshold().subtract(available);
                    alerts.add(new LowStockAlertDto(
                            p.getId(),
                            p.getSku(),
                            p.getName(),
                            b.getWarehouseId(),
                            w != null ? w.getName() : "Unknown",
                            b.getOnHandQuantity(),
                            b.getReservedQuantity(),
                            available,
                            p.getReorderThreshold(),
                            shortage.compareTo(BigDecimal.ZERO) > 0 ? shortage : BigDecimal.ZERO
                    ));
                }
            }
        }
        return alerts;
    }
}

