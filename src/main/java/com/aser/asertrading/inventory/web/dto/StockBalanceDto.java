package com.aser.asertrading.inventory.web.dto;

import com.aser.asertrading.inventory.domain.StockBalance;

import java.math.BigDecimal;
import java.time.Instant;

public record StockBalanceDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String categoryName,
        Long warehouseId,
        String warehouseName,
        Long branchId,
        String branchName,
        BigDecimal onHandQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal weightedAverageCost,
        BigDecimal totalValuation,
        String unitSymbol,
        String status,
        Instant updatedAt
) {
    public static StockBalanceDto of(
            StockBalance b,
            String sku,
            String name,
            String categoryName,
            String warehouseName,
            Long branchId,
            String branchName,
            String unitSymbol,
            BigDecimal reorderThreshold) {

        BigDecimal onHand = b.getOnHandQuantity() != null ? b.getOnHandQuantity() : BigDecimal.ZERO;
        BigDecimal cost = b.getWeightedAverageCost() != null ? b.getWeightedAverageCost() : BigDecimal.ZERO;
        BigDecimal valuation = onHand.multiply(cost).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal available = b.getAvailableQuantity() != null ? b.getAvailableQuantity() : BigDecimal.ZERO;

        String status;
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            status = "OUT_OF_STOCK";
        } else if (reorderThreshold != null && available.compareTo(reorderThreshold) <= 0) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }

        return new StockBalanceDto(
                b.getId(),
                b.getProductId(),
                sku,
                name,
                categoryName != null ? categoryName : "General",
                b.getWarehouseId(),
                warehouseName != null ? warehouseName : "Main Warehouse",
                branchId,
                branchName != null ? branchName : "Main Branch",
                onHand,
                b.getReservedQuantity() != null ? b.getReservedQuantity() : BigDecimal.ZERO,
                available,
                cost,
                valuation,
                unitSymbol != null ? unitSymbol : "piece",
                status,
                b.getUpdatedAt()
        );
    }

    public static StockBalanceDto of(
            StockBalance b,
            String sku,
            String name,
            String categoryName,
            String warehouseName,
            String branchName,
            String unitSymbol,
            BigDecimal reorderThreshold) {
        return of(b, sku, name, categoryName, warehouseName, null, branchName, unitSymbol, reorderThreshold);
    }
}
