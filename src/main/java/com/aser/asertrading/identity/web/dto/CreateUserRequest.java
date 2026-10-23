package com.aser.asertrading.identity.web.dto;

import com.aser.asertrading.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        @NotBlank(message = "Full name is required")
        String fullName,

        String phone,

        @NotNull(message = "Role is required")
        Role role,

        Long assignedBranchId,

        @NotBlank(message = "Password is required")
        @Size(min = 6)
        String password
) {}

