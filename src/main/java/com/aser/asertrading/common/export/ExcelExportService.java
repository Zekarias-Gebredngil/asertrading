package com.aser.asertrading.common.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    public record SheetData(String sheetName, List<String> headers, List<List<Object>> rows) {}

    /**
     * Builds a single-sheet Excel workbook as a byte array
     */
    public byte[] exportToExcel(String sheetTitle, List<String> headers, List<List<Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sanitizeSheetName(sheetTitle));
            applyStylesAndPopulate(workbook, sheet, headers, rows);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel export", e);
        }
    }

    /**
     * Builds a multi-sheet Excel workbook as a byte array
     */
    public byte[] exportMultiSheetExcel(List<SheetData> sheets) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (SheetData sd : sheets) {
                Sheet sheet = workbook.createSheet(sanitizeSheetName(sd.sheetName()));
                applyStylesAndPopulate(workbook, sheet, sd.headers(), sd.rows());
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate multi-sheet Excel export", e);
        }
    }

    private void applyStylesAndPopulate(Workbook workbook, Sheet sheet, List<String> headers, List<List<Object>> rows) {
        // Header Font & Style
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerFont.setFontHeightInPoints((short) 11);

        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Data Styles
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle numberStyle = workbook.createCellStyle();
        numberStyle.cloneStyleFrom(dataStyle);
        DataFormat dataFormat = workbook.createDataFormat();
        numberStyle.setDataFormat(dataFormat.getFormat("#,##0.00"));

        // Header Row
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(24);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
        }

        // Data Rows
        int rowIdx = 1;
        for (List<Object> rowData : rows) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(19);
            for (int col = 0; col < rowData.size(); col++) {
                Cell cell = row.createCell(col);
                Object val = rowData.get(col);
                if (val == null) {
                    cell.setCellValue("");
                    cell.setCellStyle(dataStyle);
                } else if (val instanceof Number num) {
                    cell.setCellValue(num.doubleValue());
                    cell.setCellStyle(numberStyle);
                } else if (val instanceof Boolean b) {
                    cell.setCellValue(b ? "Yes" : "No");
                    cell.setCellStyle(dataStyle);
                } else {
                    cell.setCellValue(String.valueOf(val));
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        // Auto-fit column widths
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(Math.max(currentWidth + 1200, 3500), 18000));
        }
    }

    private String sanitizeSheetName(String name) {
        if (name == null || name.isBlank()) return "Sheet1";
        String clean = name.replaceAll("[:\\\\/?*\\[\\]]", "");
        return clean.length() > 31 ? clean.substring(0, 31) : clean;
    }

    public String generateTimestampedFilename(String baseName) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        return baseName + "_" + ts + ".xlsx";
    }
}

