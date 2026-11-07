package com.aser.asertrading.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record StockSummaryReportDto(
        int totalProducts,
        BigDecimal totalUnitsOnHand,
        BigDecimal totalUnitsReserved,
        BigDecimal totalUnitsAvailable,
        int totalWarehouses,
        List<BranchStockSummary> branchSummaries
) {
    public record BranchStockSummary(
            Long branchId,
            String branchName,
            String branchCode,
            int warehouseCount,
            int totalProducts,
            BigDecimal totalUnitsOnHand,
            BigDecimal totalOnHand,
            BigDecimal totalReserved,
            BigDecimal totalAvailable,
            BigDecimal totalValuationEtb
    ) {}
}

