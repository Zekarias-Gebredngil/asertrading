package com.aser.asertrading.sales.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDto(
        Long cartId,
        String sessionId,
        List<CartItemDto> items,
        BigDecimal subtotal,
        int totalItems
) {}

