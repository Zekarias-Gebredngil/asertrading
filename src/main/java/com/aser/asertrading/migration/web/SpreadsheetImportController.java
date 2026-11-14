package com.aser.asertrading.migration.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.migration.service.SpreadsheetImportService;
import com.aser.asertrading.migration.web.dto.ImportJobDto;
import com.aser.asertrading.migration.web.dto.ImportValidationResultDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/imports")
@RequiredArgsConstructor
@Tag(name = "Spreadsheet Inventory Migration", description = "Endpoints for importing opening stock and products from Excel or CSV spreadsheets")
public class SpreadsheetImportController {

    private final SpreadsheetImportService importService;

    @PostMapping(value = "/inventory", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload opening inventory spreadsheet for dry-run validation preview")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<ImportValidationResultDto>> uploadSpreadsheet(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        ImportValidationResultDto result = importService.uploadAndValidate(file, auth.getName());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Spreadsheet validated", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve import job validation details and row errors")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'COMPANY_MANAGER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<ImportJobDto> getImportJob(@PathVariable Long id) {
        return ApiResponse.success(importService.getImportJobById(id));
    }

    @PostMapping("/{id}/commit")
    @Operation(summary = "Commit validated opening stock import (creates opening balances and ledger movements exactly once)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ApiResponse<ImportJobDto> commitImport(@PathVariable Long id, Authentication auth) {
        ImportJobDto job = importService.commitImport(id, auth.getName());
        return ApiResponse.success("Import committed and opening stock posted to ledger", job);
    }
}

