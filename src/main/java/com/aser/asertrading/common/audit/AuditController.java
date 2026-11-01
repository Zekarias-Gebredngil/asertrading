package com.aser.asertrading.common.audit;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Endpoints for viewing system audit records")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ACCOUNTANT_AUDITOR', 'COMPANY_MANAGER')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Search system audit records")
    public ApiResponse<PagedResponse<AuditLog>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (entityType != null && !entityType.isBlank()) {
            return ApiResponse.success(PagedResponse.of(auditService.getLogsByEntity(entityType, pageable)));
        }
        return ApiResponse.success(PagedResponse.of(auditService.getLogs(pageable)));
    }
}

