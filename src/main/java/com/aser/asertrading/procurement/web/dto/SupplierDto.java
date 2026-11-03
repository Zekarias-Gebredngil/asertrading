package com.aser.asertrading.procurement.web.dto;

import com.aser.asertrading.procurement.domain.Supplier;

import java.time.Instant;

public record SupplierDto(
        Long id,
        String name,
        String code,
        String contactPerson,
        String email,
        String phone,
        String address,
        String city,
        String taxId,
        boolean isActive,
        Instant createdAt
) {
    public static SupplierDto fromEntity(Supplier s) {
        return new SupplierDto(
                s.getId(),
                s.getName(),
                s.getCode(),
                s.getContactPerson(),
                s.getEmail(),
                s.getPhone(),
                s.getAddress(),
                s.getCity(),
                s.getTaxId(),
                s.isActive(),
                s.getCreatedAt()
        );
    }
}

