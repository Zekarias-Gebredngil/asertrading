package com.aser.asertrading.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name is required")
        String name,

        @NotBlank(message = "Category code is required")
        @Size(min = 2, max = 40)
        String code,

        Long parentId,
        String description,
        Boolean isActive
) {}

