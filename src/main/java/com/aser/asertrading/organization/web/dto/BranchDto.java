package com.aser.asertrading.organization.web.dto;

import com.aser.asertrading.organization.domain.Branch;

import java.time.Instant;
import java.util.List;

public record BranchDto(
        Long id,
        String name,
        String code,
        String address,
        String city,
        String phone,
        String email,
        boolean isActive,
        Instant createdAt,
        List<WarehouseDto> warehouses
) {
    public BranchDto(Long id, String name, String code, String address, String city, String phone, String email, boolean isActive, Instant createdAt) {
        this(id, name, code, address, city, phone, email, isActive, createdAt, List.of());
    }

    public static BranchDto fromEntity(Branch branch) {
        return fromEntity(branch, List.of());
    }

    public static BranchDto fromEntity(Branch branch, List<WarehouseDto> warehouses) {
        return new BranchDto(
                branch.getId(),
                branch.getName(),
                branch.getCode(),
                branch.getAddress(),
                branch.getCity(),
                branch.getPhone(),
                branch.getEmail(),
                branch.isActive(),
                branch.getCreatedAt(),
                warehouses != null ? warehouses : List.of()
        );
    }
}
