package com.aser.asertrading.setting.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.setting.service.SystemSettingService;
import com.aser.asertrading.setting.web.dto.SystemSettingDto;
import com.aser.asertrading.setting.web.dto.UpdateSystemSettingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "System Settings", description = "Endpoints for managing corporate parameters and system configurations")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    @Operation(summary = "Get current system settings and operational parameters")
    public ApiResponse<SystemSettingDto> getSettings() {
        return ApiResponse.success(systemSettingService.getSettings());
    }

    @PutMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Update enterprise system settings (System Administrator only)")
    public ApiResponse<SystemSettingDto> updateSettings(
            @Valid @RequestBody UpdateSystemSettingRequest request,
            Authentication authentication) {

        String actor = authentication != null ? authentication.getName() : "system";
        return ApiResponse.success("System settings updated successfully",
                systemSettingService.updateSettings(request, actor));
    }
}

