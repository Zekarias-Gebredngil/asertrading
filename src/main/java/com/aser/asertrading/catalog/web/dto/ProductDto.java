package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductAvailability;
import com.aser.asertrading.catalog.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductDto(
        Long id,
        String sku,
        String barcode,
        String name,
        String slug,
        String description,
        Long categoryId,
        String categoryName,
        Long baseUnitId,
        String baseUnitName,
        String baseUnitSymbol,
        BigDecimal purchaseCost,
        BigDecimal sellingPrice,
        BigDecimal reorderThreshold,
        Long preferredSupplierId,
        ProductStatus status,
        boolean isQuotationOnly,
        boolean isPublished,
        String imageUrl,
        String specifications,
        BigDecimal totalStockOnHand,
        BigDecimal totalStockAvailable,
        ProductAvailability availability,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductDto fromEntity(Product product, BigDecimal onHand, BigDecimal available, ProductAvailability availability) {
        return new ProductDto(
                product.getId(),
                product.getSku(),
                product.getBarcode(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getBaseUnit().getId(),
                product.getBaseUnit().getName(),
                product.getBaseUnit().getSymbol(),
                product.getPurchaseCost(),
                product.getSellingPrice(),
                product.getReorderThreshold(),
                product.getPreferredSupplierId(),
                product.getStatus(),
                product.isQuotationOnly(),
                product.isPublished(),
                product.getImageUrl(),
                product.getSpecifications(),
                onHand != null ? onHand : BigDecimal.ZERO,
                available != null ? available : BigDecimal.ZERO,
                availability,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

