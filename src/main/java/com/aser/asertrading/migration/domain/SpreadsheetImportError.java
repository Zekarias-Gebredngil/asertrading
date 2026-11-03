package com.aser.asertrading.migration.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "spreadsheet_import_errors", indexes = {
        @Index(name = "idx_import_errors_job", columnList = "job_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpreadsheetImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private SpreadsheetImportJob job;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(length = 60)
    private String sku;

    @Column(name = "field_name", length = 60)
    private String fieldName;

    @Column(name = "error_message", nullable = false, length = 500)
    private String errorMessage;
}

