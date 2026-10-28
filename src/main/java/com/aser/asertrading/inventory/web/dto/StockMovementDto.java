package com.aser.asertrading.inventory.web.dto;

import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseName,
        String branchName,
        BigDecimal quantityDelta,
        StockMovementType movementType,
        ReferenceType referenceType,
        String referenceId,
        BigDecimal unitCost,
        BigDecimal totalCost,
        String actor,
        String notes,
        String unitSymbol,
        Instant timestamp,
        Instant createdAt
) {
    public static StockMovementDto of(
            StockMovement m,
            String sku,
            String productName,
            String warehouseName,
            String branchName,
            String unitSymbol) {

        return new StockMovementDto(
                m.getId(),
                m.getProductId(),
                sku,
                productName,
                m.getWarehouseId(),
                warehouseName != null ? warehouseName : "Main Warehouse",
                branchName != null ? branchName : "Main Branch",
                m.getQuantityDelta(),
                m.getMovementType(),
                m.getReferenceType(),
                m.getReferenceId(),
                m.getUnitCost(),
                m.getTotalCost(),
                m.getActor(),
                m.getNotes(),
                unitSymbol != null ? unitSymbol : "piece",
                m.getCreatedAt(),
                m.getCreatedAt()
        );
    }
}
