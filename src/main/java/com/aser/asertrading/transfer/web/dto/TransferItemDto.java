package com.aser.asertrading.transfer.web.dto;

import com.aser.asertrading.transfer.domain.StockTransferItem;

import java.math.BigDecimal;

public record TransferItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal requestedQuantity,
        BigDecimal approvedQuantity,
        BigDecimal dispatchedQuantity,
        BigDecimal receivedQuantity
) {
    public static TransferItemDto of(StockTransferItem item, String sku, String name) {
        return new TransferItemDto(
                item.getId(),
                item.getProductId(),
                sku,
                name,
                item.getRequestedQuantity(),
                item.getApprovedQuantity(),
                item.getDispatchedQuantity(),
                item.getReceivedQuantity()
        );
    }
}

