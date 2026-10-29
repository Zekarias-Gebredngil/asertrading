package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.PaymentMethod;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email
        String customerEmail,

        String customerPhone,

        @NotNull(message = "Delivery type is required")
        DeliveryType deliveryType,

        String deliveryAddress,
        Long collectionBranchId,
        Long fulfillmentWarehouseId,

        PaymentMethod paymentMethod,
        String paymentReference,

        String notes,

        @NotEmpty(message = "Order must contain at least one product")
        List<OrderItemRequest> items
) {
    public record OrderItemRequest(
            @NotNull Long productId,
            @NotNull BigDecimal quantity
    ) {}
}

