package com.aser.asertrading.procurement.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupplierRequest(
        @NotBlank(message = "Supplier name is required")
        String name,

        @NotBlank(message = "Supplier code is required")
        @Size(min = 2, max = 40)
        String code,

        String contactPerson,
        String email,
        String phone,
        String address,
        String city,
        String taxId,
        Boolean isActive
) {}

