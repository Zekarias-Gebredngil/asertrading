package com.aser.asertrading.organization.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record BranchWarehouseRequest(
        Long id,
        String name,
        @JsonAlias({"code", "warehouseCode"})
        String code,
        Boolean isDefault,
        Boolean isActive
) {}

