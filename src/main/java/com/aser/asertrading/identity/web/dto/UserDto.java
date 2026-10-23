package com.aser.asertrading.identity.web.dto;

import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;

import java.time.Instant;

public record UserDto(
        Long id,
        String username,
        String email,
        String fullName,
        String phone,
        Role role,
        Long assignedBranchId,
        boolean isActive,
        Instant createdAt
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getRole(),
                user.getAssignedBranchId(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}

