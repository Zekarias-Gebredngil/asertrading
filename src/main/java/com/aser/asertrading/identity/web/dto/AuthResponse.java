package com.aser.asertrading.identity.web.dto;

import com.aser.asertrading.identity.domain.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String username,
        String email,
        String fullName,
        Role role,
        Long assignedBranchId
) {
    public static AuthResponse of(String token, Long userId, String username, String email, String fullName, Role role, Long branchId) {
        return new AuthResponse(token, "Bearer", userId, username, email, fullName, role, branchId);
    }
}

