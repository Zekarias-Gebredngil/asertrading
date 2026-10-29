package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.DeliveryType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteRequest(
        @NotNull(message = "Delivery type is required")
        DeliveryType deliveryType,

        Long collectionBranchId,
        String deliveryCity,

        @NotEmpty(message = "Items list cannot be empty")
        List<CartItemRequest> items
) {
    public record CartItemRequest(Long productId, BigDecimal quantity) {}
}

