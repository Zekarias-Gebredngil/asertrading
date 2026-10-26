package com.aser.asertrading.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUnitRequest(
        @NotBlank(message = "Unit name is required")
        String name,

        @NotBlank(message = "Unit code is required")
        @Size(min = 1, max = 20)
        String code,

        String symbol,
        Boolean isBaseUnit
) {}

