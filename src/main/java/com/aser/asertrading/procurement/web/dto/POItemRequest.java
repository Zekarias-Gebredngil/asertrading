package com.aser.asertrading.procurement.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record POItemRequest(
        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Unit cost is required")
        @DecimalMin(value = "0.0", message = "Unit cost cannot be negative")
        BigDecimal unitCost
) {}

