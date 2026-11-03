package com.aser.asertrading.migration.web.dto;

import java.util.List;

public record ImportValidationResultDto(
        Long jobId,
        String fileName,
        int totalRows,
        int validRows,
        int invalidRows,
        List<ImportRowData> previewRows,
        List<ImportErrorDto> errors
) {}

