package com.aser.asertrading.inventory.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.common.export.ExcelExportService;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.service.InventoryService;
import com.aser.asertrading.inventory.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory & Stock Ledger", description = "Endpoints for stock receipts, issues, adjustments, movements and balances")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ExcelExportService excelExportService;

    @GetMapping("/balances")
    @Operation(summary = "Retrieve authorized stock balances and valuations")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<StockBalanceDto>> getBalances(
            @RequestParam(required = false) Long warehouseId,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(inventoryService.getBalances(warehouseId, pageable)));
    }

    @GetMapping("/movements")
    @Operation(summary = "Search append-only stock movement history")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<StockMovementDto>> getMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockMovementType movementType,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(inventoryService.getMovements(productId, warehouseId, movementType, pageable)));
    }

    @PostMapping("/receipts")
    @Operation(summary = "Record stock receipt and recalculate weighted-average cost atomically")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER')")
    public ApiResponse<StockBalanceDto> recordReceipt(@Valid @RequestBody StockReceiptRequest request, Authentication auth) {
        StockBalanceDto balance = inventoryService.recordReceipt(request, auth.getName());
        return ApiResponse.success("Stock receipt recorded and weighted-average cost updated successfully", balance);
    }

    @PostMapping("/issues")
    @Operation(summary = "Record stock issue (enforces available stock invariant)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER', 'SALES_OFFICER')")
    public ApiResponse<StockBalanceDto> recordIssue(@Valid @RequestBody StockIssueRequest request, Authentication auth) {
        StockBalanceDto balance = inventoryService.recordIssue(request, auth.getName());
        return ApiResponse.success("Stock issue recorded successfully", balance);
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Submit an approved stock adjustment with reason code")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ApiResponse<StockBalanceDto> recordAdjustment(@Valid @RequestBody InventoryAdjustmentRequest request, Authentication auth) {
        StockBalanceDto balance = inventoryService.recordAdjustment(request, auth.getName());
        return ApiResponse.success("Inventory adjustment posted successfully", balance);
    }

    @GetMapping("/alerts/low-stock")
    @Operation(summary = "Get low stock and reorder alerts")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'PROCUREMENT_OFFICER')")
    public ApiResponse<List<LowStockAlertDto>> getLowStockAlerts(@RequestParam(required = false) Long warehouseId) {
        return ApiResponse.success(inventoryService.getLowStockAlerts(warehouseId));
    }

    @GetMapping("/export/balances")
    @Operation(summary = "Export inventory balances to Excel (all records or requested page)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ResponseEntity<byte[]> exportBalances(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<StockBalanceDto> balancesPage = inventoryService.getBalances(warehouseId, pageable);

        List<String> headers = List.of(
                "SKU", "Product Name", "Category", "Branch", "Warehouse",
                "Physical On Hand", "Reserved Qty", "Available to Sell", "Unit",
                "Weighted Avg Cost (ETB)", "Total Valuation (ETB)", "Status"
        );

        List<List<Object>> rows = balancesPage.getContent().stream().map(b -> List.<Object>of(
                b.productSku() != null ? b.productSku() : "",
                b.productName() != null ? b.productName() : "",
                b.categoryName() != null ? b.categoryName() : "General",
                b.branchName() != null ? b.branchName() : "",
                b.warehouseName() != null ? b.warehouseName() : "",
                b.onHandQuantity() != null ? b.onHandQuantity() : BigDecimal.ZERO,
                b.reservedQuantity() != null ? b.reservedQuantity() : BigDecimal.ZERO,
                b.availableQuantity() != null ? b.availableQuantity() : BigDecimal.ZERO,
                b.unitSymbol() != null ? b.unitSymbol() : "piece",
                b.weightedAverageCost() != null ? b.weightedAverageCost() : BigDecimal.ZERO,
                b.totalValuation() != null ? b.totalValuation() : BigDecimal.ZERO,
                b.status() != null ? b.status() : ""
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Stock Balances", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Stock_Balances" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/export/movements")
    @Operation(summary = "Export stock movements ledger to Excel (all records or requested page)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockMovementDto> movementsPage = inventoryService.getMovements(productId, warehouseId, movementType, pageable);

        List<String> headers = List.of(
                "Timestamp", "Movement Type", "Product SKU", "Product Name", "Branch", "Warehouse",
                "Quantity Delta", "Unit", "Unit Cost (ETB)", "Total Cost (ETB)", "Reference Type", "Reference ID", "Actor", "Notes"
        );

        List<List<Object>> rows = movementsPage.getContent().stream().map(m -> List.<Object>of(
                m.createdAt() != null ? m.createdAt().toString() : "",
                m.movementType() != null ? m.movementType().name() : "",
                m.productSku() != null ? m.productSku() : "",
                m.productName() != null ? m.productName() : "",
                m.branchName() != null ? m.branchName() : "",
                m.warehouseName() != null ? m.warehouseName() : "",
                m.quantityDelta() != null ? m.quantityDelta() : BigDecimal.ZERO,
                m.unitSymbol() != null ? m.unitSymbol() : "piece",
                m.unitCost() != null ? m.unitCost() : BigDecimal.ZERO,
                m.totalCost() != null ? m.totalCost() : BigDecimal.ZERO,
                m.referenceType() != null ? m.referenceType().name() : "",
                m.referenceId() != null ? m.referenceId() : "",
                m.actor() != null ? m.actor() : "",
                m.notes() != null ? m.notes() : ""
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Stock Movements", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Stock_Movements" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}

