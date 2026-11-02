package com.aser.asertrading.reporting.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SalesReportDto(
        long totalOrders,
        BigDecimal totalRevenueEtb,
        BigDecimal averageOrderValueEtb,
        Map<String, Long> ordersByStatus,
        long deliveryOrdersCount,
        long collectionOrdersCount
) {}

