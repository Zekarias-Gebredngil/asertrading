package com.aser.asertrading.organization.web.dto;

import com.aser.asertrading.organization.domain.Branch;

import java.time.Instant;

public record BranchDto(
        Long id,
        String name,
        String code,
        String address,
        String city,
        String phone,
        String email,
        boolean isActive,
        Instant createdAt
) {
    public static BranchDto fromEntity(Branch branch) {
        return new BranchDto(
                branch.getId(),
                branch.getName(),
                branch.getCode(),
                branch.getAddress(),
                branch.getCity(),
                branch.getPhone(),
                branch.getEmail(),
                branch.isActive(),
                branch.getCreatedAt()
        );
    }
}

