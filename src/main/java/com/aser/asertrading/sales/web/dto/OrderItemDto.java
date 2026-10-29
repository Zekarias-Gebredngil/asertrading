package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
    public static OrderItemDto fromEntity(OrderItem item) {
        return new OrderItemDto(
                item.getId(),
                item.getProductId(),
                item.getProductSku(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getTotalPrice()
        );
    }
}

