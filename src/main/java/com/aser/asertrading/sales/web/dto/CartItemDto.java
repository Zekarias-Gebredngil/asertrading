package com.aser.asertrading.sales.web.dto;

import java.math.BigDecimal;

public record CartItemDto(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String imageUrl,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {}

