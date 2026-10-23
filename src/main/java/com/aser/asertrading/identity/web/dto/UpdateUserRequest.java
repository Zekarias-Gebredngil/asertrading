package com.aser.asertrading.identity.web.dto;

import com.aser.asertrading.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        String phone,
        Role role,
        Long assignedBranchId,
        Boolean isActive,
        String newPassword
) {}

