package com.aser.asertrading.transfer.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferReceiveItemRequest(
        @NotNull(message = "Item ID is required")
        Long transferItemId,

        @NotNull(message = "Received quantity is required")
        @DecimalMin(value = "0.0", message = "Received quantity cannot be negative")
        BigDecimal receivedQuantity
) {}

