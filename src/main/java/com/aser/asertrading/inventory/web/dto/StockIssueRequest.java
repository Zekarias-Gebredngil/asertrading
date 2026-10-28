package com.aser.asertrading.inventory.web.dto;

import com.aser.asertrading.inventory.domain.ReferenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StockIssueRequest(
        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
        BigDecimal quantity,

        @NotNull(message = "Reference type is required")
        ReferenceType referenceType,

        String referenceId,
        String notes
) {}

