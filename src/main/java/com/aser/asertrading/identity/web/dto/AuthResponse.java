package com.aser.asertrading.identity.web.dto;

import com.aser.asertrading.identity.domain.Role;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,
        Long userId,
        String username,
        String email,
        String fullName,
        Role role,
        Long assignedBranchId
) {
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            Long expiresIn,
            Long userId,
            String username,
            String email,
            String fullName,
            Role role,
            Long branchId) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, userId, username, email, fullName, role, branchId);
    }

    public static AuthResponse of(
            String accessToken,
            Long userId,
            String username,
            String email,
            String fullName,
            Role role,
            Long branchId) {
        return new AuthResponse(accessToken, null, "Bearer", null, userId, username, email, fullName, role, branchId);
    }
}
