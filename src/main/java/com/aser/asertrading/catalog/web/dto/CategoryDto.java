package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.ProductCategory;

import java.time.Instant;
import java.util.List;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String code,
        Long parentId,
        String description,
        boolean isActive,
        List<CategoryDto> subCategories,
        Instant createdAt
) {
    public static CategoryDto fromEntity(ProductCategory cat) {
        List<CategoryDto> subs = cat.getSubCategories() != null
                ? cat.getSubCategories().stream().map(CategoryDto::fromEntity).toList()
                : List.of();

        return new CategoryDto(
                cat.getId(),
                cat.getName(),
                cat.getSlug(),
                cat.getCode(),
                cat.getParentCategory() != null ? cat.getParentCategory().getId() : null,
                cat.getDescription(),
                cat.isActive(),
                subs,
                cat.getCreatedAt()
        );
    }
}

