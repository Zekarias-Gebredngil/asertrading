package com.aser.asertrading.migration.web.dto;

import com.aser.asertrading.migration.domain.SpreadsheetImportError;

public record ImportErrorDto(
        Long id,
        int rowNumber,
        String sku,
        String fieldName,
        String errorMessage
) {
    public static ImportErrorDto fromEntity(SpreadsheetImportError err) {
        return new ImportErrorDto(
                err.getId(),
                err.getRowNumber(),
                err.getSku(),
                err.getFieldName(),
                err.getErrorMessage()
        );
    }
}

