package com.aser.asertrading.reporting.dto;

import java.util.Map;

public record TransferReportDto(
        long totalTransfers,
        long inTransitCount,
        long receivedCount,
        long discrepancyCount,
        Map<String, Long> transfersByStatus
) {}

