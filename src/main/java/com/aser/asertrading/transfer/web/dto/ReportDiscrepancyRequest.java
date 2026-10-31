package com.aser.asertrading.transfer.web.dto;

import com.aser.asertrading.transfer.domain.DiscrepancyType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ReportDiscrepancyRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Expected quantity is required")
        BigDecimal expectedQuantity,

        @NotNull(message = "Actual quantity is required")
        BigDecimal actualQuantity,

        @NotNull(message = "Discrepancy type is required")
        DiscrepancyType discrepancyType,

        String resolutionNotes
) {}

