package com.aser.asertrading.migration.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "spreadsheet_import_jobs", indexes = {
        @Index(name = "idx_import_jobs_status", columnList = "status"),
        @Index(name = "idx_import_jobs_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpreadsheetImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 200)
    private String fileName;

    @Column(name = "file_type", nullable = false, length = 20)
    private String fileType; // CSV, EXCEL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ImportStatus status = ImportStatus.PENDING;

    @Column(name = "total_rows")
    @Builder.Default
    private int totalRows = 0;

    @Column(name = "valid_rows")
    @Builder.Default
    private int validRows = 0;

    @Column(name = "invalid_rows")
    @Builder.Default
    private int invalidRows = 0;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "parsed_data_json", columnDefinition = "TEXT")
    private String parsedDataJson;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SpreadsheetImportError> errors = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "committed_at")
    private Instant committedAt;
}

