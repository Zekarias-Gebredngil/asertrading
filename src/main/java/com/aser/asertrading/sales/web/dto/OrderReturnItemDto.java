package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.OrderReturnItem;
import com.aser.asertrading.sales.domain.ReturnAction;

import java.math.BigDecimal;

public record OrderReturnItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        ReturnAction action
) {
    public static OrderReturnItemDto of(OrderReturnItem item, String sku, String name) {
        return new OrderReturnItemDto(
                item.getId(),
                item.getProductId(),
                sku,
                name,
                item.getQuantity(),
                item.getAction()
        );
    }
}

