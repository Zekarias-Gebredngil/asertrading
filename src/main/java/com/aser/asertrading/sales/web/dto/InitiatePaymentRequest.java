package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitiatePaymentRequest(
        @NotNull(message = "Order ID is required")
        Long orderId,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
        BigDecimal amount,

        String referenceNumber,
        String notes
) {}

