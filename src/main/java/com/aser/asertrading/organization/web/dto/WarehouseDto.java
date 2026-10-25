package com.aser.asertrading.organization.web.dto;

import com.aser.asertrading.organization.domain.Warehouse;

import java.time.Instant;

public record WarehouseDto(
        Long id,
        Long branchId,
        String branchName,
        String branchCode,
        String name,
        String code,
        boolean isDefault,
        boolean isActive,
        Instant createdAt
) {
    public static WarehouseDto fromEntity(Warehouse warehouse) {
        return new WarehouseDto(
                warehouse.getId(),
                warehouse.getBranch().getId(),
                warehouse.getBranch().getName(),
                warehouse.getBranch().getCode(),
                warehouse.getName(),
                warehouse.getCode(),
                warehouse.isDefault(),
                warehouse.isActive(),
                warehouse.getCreatedAt()
        );
    }
}

