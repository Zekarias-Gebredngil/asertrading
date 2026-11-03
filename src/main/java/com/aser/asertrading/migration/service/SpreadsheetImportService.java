package com.aser.asertrading.migration.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductCategory;
import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.domain.UnitOfMeasure;
import com.aser.asertrading.catalog.repository.ProductCategoryRepository;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.catalog.repository.UnitOfMeasureRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.inventory.service.WeightedAverageCostService;
import com.aser.asertrading.migration.domain.ImportStatus;
import com.aser.asertrading.migration.domain.SpreadsheetImportError;
import com.aser.asertrading.migration.domain.SpreadsheetImportJob;
import com.aser.asertrading.migration.repository.SpreadsheetImportJobRepository;
import com.aser.asertrading.migration.web.dto.*;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpreadsheetImportService {

    private final SpreadsheetImportJobRepository jobRepository;
    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final WeightedAverageCostService costService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ImportValidationResultDto uploadAndValidate(MultipartFile file, String actor) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Uploaded file name is invalid");
        }

        String lower = originalFilename.toLowerCase();
        boolean isCsv = lower.endsWith(".csv");
        boolean isExcel = lower.endsWith(".xlsx") || lower.endsWith(".xls");

        if (!isCsv && !isExcel) {
            throw new BadRequestException("Only CSV (.csv) and Excel (.xlsx, .xls) files are supported");
        }

        List<ImportRowData> parsedRows;
        try {
            if (isCsv) {
                parsedRows = parseCsv(file.getInputStream());
            } else {
                parsedRows = parseExcel(file.getInputStream());
            }
        } catch (Exception e) {
            log.error("Failed to parse spreadsheet: {}", e.getMessage(), e);
            throw new BadRequestException("Could not parse file: " + e.getMessage());
        }

        if (parsedRows.isEmpty()) {
            throw new BadRequestException("The uploaded spreadsheet does not contain any data rows");
        }

        // Validate rows against database references
        List<SpreadsheetImportError> errors = new ArrayList<>();
        int validCount = 0;
        int invalidCount = 0;

        List<ImportRowData> validatedRows = new ArrayList<>();
        for (ImportRowData rawRow : parsedRows) {
            StringBuilder errorMsg = new StringBuilder();

            if (rawRow.sku() == null || rawRow.sku().isBlank()) {
                errorMsg.append("SKU is required. ");
            }
            if (rawRow.name() == null || rawRow.name().isBlank()) {
                errorMsg.append("Product name is required. ");
            }
            if (rawRow.categoryCode() == null || !categoryRepository.existsByCode(rawRow.categoryCode().trim().toUpperCase())) {
                errorMsg.append("Invalid or unknown category code '").append(rawRow.categoryCode()).append("'. ");
            }
            if (rawRow.unitCode() == null || !unitRepository.existsByCode(rawRow.unitCode().trim().toUpperCase())) {
                errorMsg.append("Invalid or unknown unit code '").append(rawRow.unitCode()).append("'. ");
            }
            if (rawRow.warehouseCode() == null || !warehouseRepository.existsByCode(rawRow.warehouseCode().trim().toUpperCase())) {
                errorMsg.append("Invalid or unknown warehouse code '").append(rawRow.warehouseCode()).append("'. ");
            }
            if (rawRow.quantity() == null || rawRow.quantity().compareTo(BigDecimal.ZERO) < 0) {
                errorMsg.append("Quantity must be zero or positive. ");
            }
            if (rawRow.unitCost() == null || rawRow.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                errorMsg.append("Unit cost must be zero or positive. ");
            }

            boolean isValid = errorMsg.isEmpty();
            String err = isValid ? null : errorMsg.toString().trim();

            if (isValid) {
                validCount++;
            } else {
                invalidCount++;
            }

            ImportRowData validatedRow = new ImportRowData(
                    rawRow.rowNumber(),
                    rawRow.sku(),
                    rawRow.name(),
                    rawRow.categoryCode(),
                    rawRow.unitCode(),
                    rawRow.warehouseCode(),
                    rawRow.quantity(),
                    rawRow.unitCost(),
                    rawRow.sellingPrice(),
                    rawRow.reorderThreshold(),
                    isValid,
                    err
            );
            validatedRows.add(validatedRow);
        }

        SpreadsheetImportJob job = SpreadsheetImportJob.builder()
                .fileName(originalFilename)
                .fileType(isCsv ? "CSV" : "EXCEL")
                .status(ImportStatus.VALIDATED)
                .totalRows(validatedRows.size())
                .validRows(validCount)
                .invalidRows(invalidCount)
                .uploadedBy(actor)
                .notes("Dry-run validation complete. " + validCount + " valid rows, " + invalidCount + " invalid rows.")
                .build();

        for (ImportRowData row : validatedRows) {
            if (!row.isValid()) {
                SpreadsheetImportError err = SpreadsheetImportError.builder()
                        .job(job)
                        .rowNumber(row.rowNumber())
                        .sku(row.sku())
                        .errorMessage(row.errorMessage())
                        .build();
                job.getErrors().add(err);
                errors.add(err);
            }
        }

        try {
            job.setParsedDataJson(objectMapper.writeValueAsString(validatedRows));
        } catch (Exception e) {
            log.warn("Failed to serialize parsed rows to JSON: {}", e.getMessage());
        }

        job = jobRepository.save(job);

        auditService.log(actor, "VALIDATE_IMPORT", "SpreadsheetImportJob", job.getId().toString(),
                String.format("Validated file %s: %d total, %d valid, %d invalid", originalFilename, validatedRows.size(), validCount, invalidCount), null);

        List<ImportErrorDto> errorDtos = errors.stream().map(ImportErrorDto::fromEntity).toList();
        List<ImportRowData> previewRows = validatedRows.stream().limit(25).toList();

        return new ImportValidationResultDto(job.getId(), job.getFileName(), job.getTotalRows(), validCount, invalidCount, previewRows, errorDtos);
    }

    @Transactional
    public ImportJobDto commitImport(Long jobId, String actor) {
        SpreadsheetImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found with id: " + jobId));

        // Requirement 17.5 #37: Duplicate import commits are prevented
        if (job.getStatus() == ImportStatus.COMMITTED) {
            throw new BadRequestException("This import job has already been committed and cannot be re-applied");
        }

        if (job.getStatus() != ImportStatus.VALIDATED) {
            throw new BadRequestException("Job is not in VALIDATED state. Current status: " + job.getStatus());
        }

        if (job.getParsedDataJson() == null || job.getParsedDataJson().isBlank()) {
            throw new BadRequestException("No parsed row data available for this import job");
        }

        List<ImportRowData> rows;
        try {
            rows = objectMapper.readValue(job.getParsedDataJson(), new TypeReference<List<ImportRowData>>() {});
        } catch (Exception e) {
            throw new BadRequestException("Failed to read parsed row data: " + e.getMessage());
        }

        int committedCount = 0;

        for (ImportRowData row : rows) {
            if (!row.isValid()) {
                continue;
            }

            String sku = row.sku().trim().toUpperCase();
            ProductCategory category = categoryRepository.findByCode(row.categoryCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + row.categoryCode()));

            UnitOfMeasure baseUnit = unitRepository.findByCode(row.unitCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Unit not found: " + row.unitCode()));

            Warehouse warehouse = warehouseRepository.findByCode(row.warehouseCode().trim().toUpperCase())
                    .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + row.warehouseCode()));

            // Find or create product
            Product product = productRepository.findBySku(sku).orElseGet(() -> {
                String slug = row.name().toLowerCase().replaceAll("[^a-z0-9]", "-") + "-" + sku.toLowerCase();
                return productRepository.save(Product.builder()
                        .sku(sku)
                        .name(row.name().trim())
                        .slug(slug)
                        .category(category)
                        .baseUnit(baseUnit)
                        .purchaseCost(row.unitCost() != null ? row.unitCost() : BigDecimal.ZERO)
                        .sellingPrice(row.sellingPrice() != null ? row.sellingPrice() : BigDecimal.ZERO)
                        .reorderThreshold(row.reorderThreshold() != null ? row.reorderThreshold() : BigDecimal.valueOf(10))
                        .status(ProductStatus.ACTIVE)
                        .isPublished(true)
                        .build());
            });

            BigDecimal openingQty = row.quantity() != null ? row.quantity() : BigDecimal.ZERO;
            BigDecimal openingCost = row.unitCost() != null ? row.unitCost() : BigDecimal.ZERO;

            if (openingQty.compareTo(BigDecimal.ZERO) > 0) {
                StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                        .orElseGet(() -> StockBalance.builder()
                                .productId(product.getId())
                                .warehouseId(warehouse.getId())
                                .onHandQuantity(BigDecimal.ZERO)
                                .reservedQuantity(BigDecimal.ZERO)
                                .weightedAverageCost(openingCost)
                                .build());

                BigDecimal newAvgCost = costService.calculateNewAverageCost(
                        balance.getOnHandQuantity(), balance.getWeightedAverageCost(),
                        openingQty, openingCost
                );

                balance.setOnHandQuantity(balance.getOnHandQuantity().add(openingQty));
                balance.setWeightedAverageCost(newAvgCost);
                stockBalanceRepository.save(balance);

                BigDecimal totalCost = openingQty.multiply(openingCost).setScale(4, java.math.RoundingMode.HALF_UP);

                StockMovement movement = StockMovement.builder()
                        .productId(product.getId())
                        .warehouseId(warehouse.getId())
                        .quantityDelta(openingQty)
                        .movementType(StockMovementType.OPENING_BALANCE)
                        .referenceType(ReferenceType.IMPORT)
                        .referenceId("JOB-" + job.getId())
                        .unitCost(openingCost)
                        .totalCost(totalCost)
                        .actor(actor)
                        .notes("Opening inventory imported from " + job.getFileName())
                        .build();

                stockMovementRepository.save(movement);
            }

            committedCount++;
        }

        job.setStatus(ImportStatus.COMMITTED);
        job.setCommittedAt(Instant.now());
        job.setNotes("Successfully committed " + committedCount + " inventory balance records.");
        job = jobRepository.save(job);

        auditService.log(actor, "COMMIT_IMPORT", "SpreadsheetImportJob", job.getId().toString(),
                "Committed import job " + job.getId() + " (" + committedCount + " rows posted to stock ledger)", null);

        return ImportJobDto.fromEntity(job);
    }

    @Transactional(readOnly = true)
    public ImportJobDto getImportJobById(Long id) {
        SpreadsheetImportJob job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found with id: " + id));
        return ImportJobDto.fromEntity(job);
    }

    private List<ImportRowData> parseCsv(InputStream is) throws Exception {
        List<ImportRowData> list = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build())) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                String sku = getRecordValue(record, "sku");
                String name = getRecordValue(record, "name");
                String catCode = getRecordValue(record, "categoryCode");
                String uomCode = getRecordValue(record, "unitCode");
                String whCode = getRecordValue(record, "warehouseCode");
                BigDecimal qty = parseDecimal(getRecordValue(record, "quantity"), BigDecimal.ZERO);
                BigDecimal cost = parseDecimal(getRecordValue(record, "unitCost"), BigDecimal.ZERO);
                BigDecimal price = parseDecimal(getRecordValue(record, "sellingPrice"), BigDecimal.ZERO);
                BigDecimal threshold = parseDecimal(getRecordValue(record, "reorderThreshold"), BigDecimal.valueOf(10));

                list.add(new ImportRowData(rowNum, sku, name, catCode, uomCode, whCode, qty, cost, price, threshold, true, null));
            }
        }
        return list;
    }

    private List<ImportRowData> parseExcel(InputStream is) throws Exception {
        List<ImportRowData> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("Excel sheet has no header row");
            }

            Map<String, Integer> colMap = new HashMap<>();
            for (Cell cell : headerRow) {
                colMap.put(cell.getStringCellValue().trim().toLowerCase(), cell.getColumnIndex());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String sku = getCellValue(row, colMap.get("sku"));
                if (sku == null || sku.isBlank()) continue; // skip empty rows

                String name = getCellValue(row, colMap.get("name"));
                String catCode = getCellValue(row, colMap.get("categorycode"));
                String uomCode = getCellValue(row, colMap.get("unitcode"));
                String whCode = getCellValue(row, colMap.get("warehousecode"));
                BigDecimal qty = parseDecimal(getCellValue(row, colMap.get("quantity")), BigDecimal.ZERO);
                BigDecimal cost = parseDecimal(getCellValue(row, colMap.get("unitcost")), BigDecimal.ZERO);
                BigDecimal price = parseDecimal(getCellValue(row, colMap.get("sellingprice")), BigDecimal.ZERO);
                BigDecimal threshold = parseDecimal(getCellValue(row, colMap.get("reorderthreshold")), BigDecimal.valueOf(10));

                list.add(new ImportRowData(r + 1, sku, name, catCode, uomCode, whCode, qty, cost, price, threshold, true, null));
            }
        }
        return list;
    }

    private String getRecordValue(CSVRecord record, String col) {
        try {
            return record.get(col);
        } catch (Exception e) {
            return null;
        }
    }

    private String getCellValue(Row row, Integer colIdx) {
        if (colIdx == null) return null;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return null;
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private BigDecimal parseDecimal(String str, BigDecimal defaultVal) {
        if (str == null || str.isBlank()) return defaultVal;
        try {
            return new BigDecimal(str.replaceAll(",", ""));
        } catch (Exception e) {
            return defaultVal;
        }
    }
}

