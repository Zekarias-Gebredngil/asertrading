package com.aser.asertrading.inventory.web.dto;

import java.math.BigDecimal;

public record LowStockAlertDto(
        Long productId,
        String productSku,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal onHandQuantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        BigDecimal reorderThreshold,
        BigDecimal shortage
) {}

