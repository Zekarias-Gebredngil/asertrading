package com.aser.asertrading.procurement.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePORequest(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @NotNull(message = "Destination warehouse ID is required")
        Long warehouseId,

        String notes,

        @NotEmpty(message = "Purchase order must contain at least one item")
        @Valid
        List<POItemRequest> items
) {}

