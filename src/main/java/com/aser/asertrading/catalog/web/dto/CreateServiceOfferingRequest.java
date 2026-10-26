package com.aser.asertrading.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateServiceOfferingRequest(
        @NotBlank(message = "Service code is required")
        @Size(min = 2, max = 50)
        String serviceCode,

        @NotBlank(message = "Title is required")
        @Size(min = 2, max = 200)
        String title,

        @NotBlank(message = "Category is required")
        @Size(min = 2, max = 100)
        String category,

        String description,
        String imageUrl,
        List<String> capabilities,
        Boolean requiresQuotation,
        Integer displayOrder
) {}

