package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductAvailability;

import java.math.BigDecimal;

public record PublicProductDto(
        Long id,
        String sku,
        String name,
        String slug,
        String description,
        Long categoryId,
        String categoryName,
        String baseUnitName,
        String baseUnitSymbol,
        BigDecimal sellingPrice,
        boolean isQuotationOnly,
        String imageUrl,
        String specifications,
        ProductAvailability availability
) {
    public static PublicProductDto fromEntity(Product product, ProductAvailability availability) {
        return new PublicProductDto(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBaseUnit().getName(),
                product.getBaseUnit().getSymbol(),
                product.isQuotationOnly() ? null : product.getSellingPrice(),
                product.isQuotationOnly(),
                product.getImageUrl(),
                product.getSpecifications(),
                availability
        );
    }
}

