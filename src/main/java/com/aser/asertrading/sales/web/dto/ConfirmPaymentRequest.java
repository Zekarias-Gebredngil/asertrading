package com.aser.asertrading.sales.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank(message = "Reference / receipt number is required")
        String referenceNumber,

        String notes
) {}

