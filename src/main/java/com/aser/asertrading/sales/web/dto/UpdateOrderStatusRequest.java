package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Target status is required")
        OrderStatus status,

        String comment,

        String paymentStatus
) {}

