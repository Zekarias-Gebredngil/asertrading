package com.aser.asertrading.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWarehouseRequest(
        @NotNull(message = "Branch ID is required")
        Long branchId,

        @NotBlank(message = "Warehouse name is required")
        String name,

        @NotBlank(message = "Warehouse code is required")
        @Size(min = 2, max = 30)
        String code,

        Boolean isDefault,
        Boolean isActive
) {}

