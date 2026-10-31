package com.aser.asertrading.transfer.web.dto;

import com.aser.asertrading.transfer.domain.DiscrepancyType;
import com.aser.asertrading.transfer.domain.TransferDiscrepancy;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferDiscrepancyDto(
        Long id,
        Long transferId,
        Long productId,
        String productSku,
        String productName,
        BigDecimal expectedQuantity,
        BigDecimal actualQuantity,
        BigDecimal difference,
        DiscrepancyType discrepancyType,
        boolean isResolved,
        String resolutionNotes,
        String reportedBy,
        Instant createdAt
) {
    public static TransferDiscrepancyDto of(TransferDiscrepancy d, String sku, String name) {
        BigDecimal diff = d.getActualQuantity().subtract(d.getExpectedQuantity());
        return new TransferDiscrepancyDto(
                d.getId(),
                d.getTransferId(),
                d.getProductId(),
                sku,
                name,
                d.getExpectedQuantity(),
                d.getActualQuantity(),
                diff,
                d.getDiscrepancyType(),
                d.isResolved(),
                d.getResolutionNotes(),
                d.getReportedBy(),
                d.getCreatedAt()
        );
    }
}

