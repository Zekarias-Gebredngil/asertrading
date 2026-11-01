package com.aser.asertrading.setting.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateSystemSettingRequest(
        @NotBlank(message = "Company name is required")
        String companyName,

        @NotBlank(message = "Amharic legal name is required")
        String amharicName,

        String tinNumber,
        String address,
        String phone,
        String email,
        String supportEmail,
        String currency,

        @NotNull(message = "Low stock threshold is required")
        Integer lowStockThreshold,

        @NotNull(message = "Order timeout is required")
        Integer orderTimeoutHours,

        Boolean autoFulfillReservations,
        Boolean enforceStrictBranchLockdown,
        String valuationMethod,

        Integer sessionTimeoutHours,
        Integer minPasswordLength,
        Boolean requireAuditLogging,
        String allowedOrigins
) {}

