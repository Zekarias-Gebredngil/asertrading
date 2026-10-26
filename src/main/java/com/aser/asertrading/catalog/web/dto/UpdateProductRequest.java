package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(min = 2, max = 200)
        String name,

        String barcode,
        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Base unit ID is required")
        Long baseUnitId,

        @PositiveOrZero(message = "Purchase cost must be zero or positive")
        BigDecimal purchaseCost,

        @PositiveOrZero(message = "Selling price must be zero or positive")
        BigDecimal sellingPrice,

        @PositiveOrZero(message = "Reorder threshold must be zero or positive")
        BigDecimal reorderThreshold,

        Long preferredSupplierId,
        ProductStatus status,
        Boolean isQuotationOnly,
        Boolean isPublished,
        String imageUrl,
        String specifications
) {}

