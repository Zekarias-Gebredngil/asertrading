package com.aser.asertrading.organization.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBranchRequest(
        @NotBlank(message = "Branch name is required")
        String name,

        @NotBlank(message = "Branch code is required")
        @Size(min = 2, max = 30)
        String code,

        String address,
        String city,
        String phone,
        String email,
        Boolean isActive
) {}

