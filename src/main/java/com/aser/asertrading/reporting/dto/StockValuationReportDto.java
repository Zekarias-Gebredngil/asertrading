package com.aser.asertrading.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record StockValuationReportDto(
        BigDecimal totalValuationEtb,
        int productCount,
        List<ProductValuationItem> productValuations
) {
    public record ProductValuationItem(
            Long productId,
            String sku,
            String productName,
            String categoryName,
            String warehouseName,
            BigDecimal onHandQuantity,
            BigDecimal weightedAverageCost,
            BigDecimal totalValuation
    ) {}
}

