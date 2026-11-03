package com.aser.asertrading.migration.web.dto;

import java.math.BigDecimal;

public record ImportRowData(
        int rowNumber,
        String sku,
        String name,
        String categoryCode,
        String unitCode,
        String warehouseCode,
        BigDecimal quantity,
        BigDecimal unitCost,
        BigDecimal sellingPrice,
        BigDecimal reorderThreshold,
        boolean isValid,
        String errorMessage
) {}

