package com.aser.asertrading.setting.web.dto;

import com.aser.asertrading.setting.domain.SystemSetting;

import java.time.Instant;

public record SystemSettingDto(
        Long id,
        String companyName,
        String amharicName,
        String tinNumber,
        String address,
        String phone,
        String email,
        String supportEmail,
        String currency,
        Integer lowStockThreshold,
        Integer orderTimeoutHours,
        Boolean autoFulfillReservations,
        Boolean enforceStrictBranchLockdown,
        String valuationMethod,
        Integer sessionTimeoutHours,
        Integer minPasswordLength,
        Boolean requireAuditLogging,
        String allowedOrigins,
        Instant updatedAt,
        String updatedBy
) {
    public static SystemSettingDto fromEntity(SystemSetting s) {
        if (s == null) return null;
        return new SystemSettingDto(
                s.getId(),
                s.getCompanyName(),
                s.getAmharicName(),
                s.getTinNumber(),
                s.getAddress(),
                s.getPhone(),
                s.getEmail(),
                s.getSupportEmail(),
                s.getCurrency(),
                s.getLowStockThreshold(),
                s.getOrderTimeoutHours(),
                s.getAutoFulfillReservations(),
                s.getEnforceStrictBranchLockdown(),
                s.getValuationMethod(),
                s.getSessionTimeoutHours(),
                s.getMinPasswordLength(),
                s.getRequireAuditLogging(),
                s.getAllowedOrigins(),
                s.getUpdatedAt(),
                s.getUpdatedBy()
        );
    }
}

