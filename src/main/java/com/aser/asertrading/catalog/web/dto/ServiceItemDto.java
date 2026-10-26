package com.aser.asertrading.catalog.web.dto;

import com.aser.asertrading.catalog.domain.ServiceOffering;

import java.util.Arrays;
import java.util.List;

public record ServiceItemDto(
        Long id,
        String serviceCode,
        String title,
        String category,
        String description,
        String imageUrl,
        List<String> capabilities,
        boolean requiresQuotation,
        boolean isActive,
        Integer displayOrder
) {
    public static ServiceItemDto fromEntity(ServiceOffering entity) {
        List<String> caps = entity.getKeyOfferings() != null && !entity.getKeyOfferings().isBlank()
                ? Arrays.stream(entity.getKeyOfferings().split("[;,]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
                : List.of();

        return new ServiceItemDto(
                entity.getId(),
                entity.getCode(),
                entity.getTitle(),
                entity.getCategory(),
                entity.getDescription(),
                entity.getImageUrl(),
                caps,
                entity.isQuotationEnabled(),
                entity.isActive(),
                entity.getDisplayOrder()
        );
    }
}
