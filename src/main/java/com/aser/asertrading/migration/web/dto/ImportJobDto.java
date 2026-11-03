package com.aser.asertrading.migration.web.dto;

import com.aser.asertrading.migration.domain.ImportStatus;
import com.aser.asertrading.migration.domain.SpreadsheetImportJob;

import java.time.Instant;
import java.util.List;

public record ImportJobDto(
        Long id,
        String fileName,
        String fileType,
        ImportStatus status,
        int totalRows,
        int validRows,
        int invalidRows,
        String uploadedBy,
        String notes,
        List<ImportErrorDto> errors,
        Instant createdAt,
        Instant committedAt
) {
    public static ImportJobDto fromEntity(SpreadsheetImportJob job) {
        List<ImportErrorDto> errorDtos = job.getErrors() != null
                ? job.getErrors().stream().map(ImportErrorDto::fromEntity).toList()
                : List.of();

        return new ImportJobDto(
                job.getId(),
                job.getFileName(),
                job.getFileType(),
                job.getStatus(),
                job.getTotalRows(),
                job.getValidRows(),
                job.getInvalidRows(),
                job.getUploadedBy(),
                job.getNotes(),
                errorDtos,
                job.getCreatedAt(),
                job.getCommittedAt()
        );
    }
}

