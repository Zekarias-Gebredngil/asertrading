package com.aser.asertrading.migration.repository;

import com.aser.asertrading.migration.domain.ImportStatus;
import com.aser.asertrading.migration.domain.SpreadsheetImportJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpreadsheetImportJobRepository extends JpaRepository<SpreadsheetImportJob, Long> {
    Page<SpreadsheetImportJob> findByStatus(ImportStatus status, Pageable pageable);
}

