package com.aser.asertrading.setting.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.setting.domain.SystemSetting;
import com.aser.asertrading.setting.repository.SystemSettingRepository;
import com.aser.asertrading.setting.web.dto.SystemSettingDto;
import com.aser.asertrading.setting.web.dto.UpdateSystemSettingRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final AuditService auditService;

    @Transactional
    public SystemSettingDto getSettings() {
        SystemSetting setting = systemSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSetting);
        return SystemSettingDto.fromEntity(setting);
    }

    @Transactional
    public SystemSettingDto updateSettings(UpdateSystemSettingRequest request, String actor) {
        SystemSetting setting = systemSettingRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createDefaultSetting);

        setting.setCompanyName(request.companyName().trim());
        setting.setAmharicName(request.amharicName().trim());
        setting.setTinNumber(request.tinNumber() != null ? request.tinNumber().trim() : null);
        setting.setAddress(request.address() != null ? request.address().trim() : null);
        setting.setPhone(request.phone() != null ? request.phone().trim() : null);
        setting.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);
        setting.setSupportEmail(request.supportEmail() != null ? request.supportEmail().trim().toLowerCase() : null);
        if (request.currency() != null && !request.currency().isBlank()) {
            setting.setCurrency(request.currency().trim());
        }

        if (request.lowStockThreshold() != null) {
            setting.setLowStockThreshold(request.lowStockThreshold());
        }
        if (request.orderTimeoutHours() != null) {
            setting.setOrderTimeoutHours(request.orderTimeoutHours());
        }
        if (request.autoFulfillReservations() != null) {
            setting.setAutoFulfillReservations(request.autoFulfillReservations());
        }
        if (request.enforceStrictBranchLockdown() != null) {
            setting.setEnforceStrictBranchLockdown(request.enforceStrictBranchLockdown());
        }
        if (request.valuationMethod() != null && !request.valuationMethod().isBlank()) {
            setting.setValuationMethod(request.valuationMethod().trim());
        }

        if (request.sessionTimeoutHours() != null) {
            setting.setSessionTimeoutHours(request.sessionTimeoutHours());
        }
        if (request.minPasswordLength() != null) {
            setting.setMinPasswordLength(request.minPasswordLength());
        }
        if (request.requireAuditLogging() != null) {
            setting.setRequireAuditLogging(request.requireAuditLogging());
        }
        if (request.allowedOrigins() != null && !request.allowedOrigins().isBlank()) {
            setting.setAllowedOrigins(request.allowedOrigins().trim());
        }

        setting.setUpdatedAt(Instant.now());
        setting.setUpdatedBy(actor != null ? actor : "system");

        setting = systemSettingRepository.save(setting);

        auditService.log(actor != null ? actor : "system", "UPDATE_SETTINGS", "SystemSetting",
                setting.getId().toString(), "Updated enterprise system settings and operational parameters", null);

        return SystemSettingDto.fromEntity(setting);
    }

    private SystemSetting createDefaultSetting() {
        SystemSetting defaultSetting = SystemSetting.builder()
                .companyName("Aser Trading")
                .amharicName("ኣሰር ትሬዲንግ (Aser Trading PLC)")
                .tinNumber("0012948571")
                .address("Bole Medhanialem, Commercial Tower 4th Floor, Addis Ababa, Ethiopia")
                .phone("+251 11 662 4545")
                .email("info@asertrading.et")
                .supportEmail("support@asertrading.et")
                .currency("ETB (Ethiopian Birr - ብር)")
                .lowStockThreshold(10)
                .orderTimeoutHours(48)
                .autoFulfillReservations(true)
                .enforceStrictBranchLockdown(true)
                .valuationMethod("WEIGHTED_AVERAGE_COST")
                .sessionTimeoutHours(24)
                .minPasswordLength(6)
                .requireAuditLogging(true)
                .allowedOrigins("http://localhost:4200, http://127.0.0.1:4200")
                .updatedAt(Instant.now())
                .updatedBy("system")
                .build();

        return systemSettingRepository.save(defaultSetting);
    }
}

