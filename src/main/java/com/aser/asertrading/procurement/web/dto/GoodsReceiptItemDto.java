package com.aser.asertrading.procurement.web.dto;

import com.aser.asertrading.procurement.domain.GoodsReceiptItem;

import java.math.BigDecimal;

public record GoodsReceiptItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantityReceived,
        BigDecimal unitCost
) {
    public static GoodsReceiptItemDto of(GoodsReceiptItem item, String sku, String name) {
        return new GoodsReceiptItemDto(
                item.getId(),
                item.getProductId(),
                sku,
                name,
                item.getQuantityReceived(),
                item.getUnitCost()
        );
    }
}

