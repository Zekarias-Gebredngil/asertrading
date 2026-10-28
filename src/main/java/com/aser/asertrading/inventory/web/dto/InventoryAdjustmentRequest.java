package com.aser.asertrading.inventory.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InventoryAdjustmentRequest(
        @NotNull(message = "Warehouse ID is required")
        Long warehouseId,

        @NotNull(message = "Product ID is required")
        Long productId,

        @NotNull(message = "Quantity delta is required")
        BigDecimal quantityDelta,

        @NotBlank(message = "Reason code is required")
        String reasonCode, // DAMAGE, EXPIRY, CORRECTION, SCRAP, FOUND

        String notes
) {}

