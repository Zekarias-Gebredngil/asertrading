package com.aser.asertrading.common.export;

import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.service.ProductService;
import com.aser.asertrading.catalog.web.dto.ProductDto;
import com.aser.asertrading.common.audit.AuditLog;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.service.InventoryService;
import com.aser.asertrading.inventory.web.dto.StockBalanceDto;
import com.aser.asertrading.inventory.web.dto.StockMovementDto;
import com.aser.asertrading.reporting.dto.SalesReportDto;
import com.aser.asertrading.reporting.dto.StockSummaryReportDto;
import com.aser.asertrading.reporting.dto.StockValuationReportDto;
import com.aser.asertrading.reporting.dto.TransferReportDto;
import com.aser.asertrading.reporting.service.ReportingService;
import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.service.OrderService;
import com.aser.asertrading.sales.web.dto.OrderDto;
import com.aser.asertrading.transfer.domain.TransferStatus;
import com.aser.asertrading.transfer.service.TransferWorkflowService;
import com.aser.asertrading.transfer.web.dto.StockTransferDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Data Export Engine", description = "Endpoints for downloading Excel (.xlsx) reports across all system contents or requested pages")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR', 'SALES_OFFICER')")
public class ExportController {

    private final ExcelExportService excelExportService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ProductService productService;
    private final TransferWorkflowService transferWorkflowService;
    private final AuditService auditService;
    private final ReportingService reportingService;

    private static final String EXCEL_MEDIA_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private List<Object> row(Object... items) {
        List<Object> r = new ArrayList<>(items.length);
        for (Object item : items) {
            r.add(item != null ? item : "");
        }
        return r;
    }

    @GetMapping("/inventory")
    @Operation(summary = "Export inventory balances to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportInventory(
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

        List<List<Object>> rows = balancesPage.getContent().stream().map(b -> row(
                b.productSku(),
                b.productName(),
                b.categoryName(),
                b.branchName(),
                b.warehouseName(),
                b.onHandQuantity(),
                b.reservedQuantity(),
                b.availableQuantity(),
                b.unitSymbol(),
                b.weightedAverageCost(),
                b.totalValuation(),
                b.status()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Stock Balances", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Inventory_Balances" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/movements")
    @Operation(summary = "Export stock movements to Excel (all records or requested page)")
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

        List<List<Object>> rows = movementsPage.getContent().stream().map(m -> row(
                m.createdAt(),
                m.movementType() != null ? m.movementType().name() : "",
                m.productSku(),
                m.productName(),
                m.branchName(),
                m.warehouseName(),
                m.quantityDelta(),
                m.unitSymbol(),
                m.unitCost(),
                m.totalCost(),
                m.referenceType() != null ? m.referenceType().name() : "",
                m.referenceId(),
                m.actor(),
                m.notes()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Stock Movements", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Stock_Movements" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/orders")
    @Operation(summary = "Export orders to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderDto> ordersPage = orderService.getOrders(status, branchId, pageable);

        List<String> headers = List.of(
                "Order Number", "Date Created", "Customer Name", "Customer Phone", "Customer Email",
                "Fulfillment Type", "Branch / Collection", "Delivery Address", "Status",
                "Subtotal (ETB)", "Delivery Fee (ETB)", "Total Amount (ETB)", "Payment Method", "Payment Status"
        );

        List<List<Object>> rows = ordersPage.getContent().stream().map(o -> row(
                o.orderNumber(),
                o.createdAt(),
                o.customerName(),
                o.customerPhone(),
                o.customerEmail(),
                o.deliveryType() != null ? o.deliveryType().name() : "",
                o.collectionBranchName(),
                o.deliveryAddress(),
                o.status() != null ? o.status().name() : "",
                o.subtotal(),
                o.deliveryFee(),
                o.totalAmount(),
                o.paymentMethod(),
                o.paymentStatus()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Customer Orders", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Orders" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/products")
    @Operation(summary = "Export product catalog to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean quotationOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<ProductDto> productsPage = productService.searchProducts(status, categoryId, quotationOnly, search, pageable);

        List<String> headers = List.of(
                "SKU", "Product Name", "Category", "Unit", "Base Cost (ETB)", "Selling Price (ETB)",
                "Pricing Model", "Availability", "Reorder Threshold", "Total Stock On Hand", "Published", "Status"
        );

        List<List<Object>> rows = productsPage.getContent().stream().map(p -> row(
                p.sku(),
                p.name(),
                p.categoryName(),
                p.baseUnitSymbol() != null ? p.baseUnitSymbol() : (p.baseUnitName() != null ? p.baseUnitName() : "piece"),
                p.purchaseCost(),
                p.sellingPrice(),
                p.isQuotationOnly() ? "Quotation" : "Fixed",
                p.availability() != null ? p.availability().name() : "",
                p.reorderThreshold(),
                p.totalStockOnHand(),
                p.isPublished() ? "Published" : "Draft",
                p.status() != null ? p.status().name() : ""
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Products Catalog", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Products_Catalog" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/transfers")
    @Operation(summary = "Export inter-branch transfers to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportTransfers(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<StockTransferDto> transfersPage = transferWorkflowService.getTransfers(status, pageable);

        List<String> headers = List.of(
                "Transfer #", "Source Warehouse", "Destination Warehouse", "Status",
                "Items Count", "Requested By", "Approved By", "Dispatched By", "Received By", "Notes"
        );

        List<List<Object>> rows = transfersPage.getContent().stream().map(t -> row(
                t.transferNumber(),
                t.sourceWarehouseName(),
                t.destinationWarehouseName(),
                t.status() != null ? t.status().name() : "",
                t.items() != null ? t.items().size() : 0,
                t.requestedBy(),
                t.approvedBy(),
                t.dispatchedBy(),
                t.receivedBy(),
                t.notes()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Branch Transfers", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Stock_Transfers" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Export audit logs to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> logsPage = (entityType != null && !entityType.isBlank())
                ? auditService.getLogsByEntity(entityType, pageable)
                : auditService.getLogs(pageable);

        List<String> headers = List.of(
                "Record ID", "Timestamp", "Actor", "Action", "Entity Module", "Entity ID", "Forensic Details", "IP Address"
        );

        List<List<Object>> rows = logsPage.getContent().stream().map(l -> row(
                l.getId(),
                l.getCreatedAt(),
                l.getActor(),
                l.getAction(),
                l.getEntityType(),
                l.getEntityId(),
                l.getDetails(),
                l.getIpAddress()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("System Audit Trail", headers, rows);
        String filename = excelExportService.generateTimestampedFilename("Aser_Audit_Logs" + (exportAll ? "_All" : "_Page_" + (page + 1)));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/reports")
    @Operation(summary = "Export full commercial intelligence multi-sheet report to Excel")
    public ResponseEntity<byte[]> exportFullCommercialReport() {
        SalesReportDto sales = reportingService.getSalesReport();
        StockSummaryReportDto stock = reportingService.getStockSummary();
        StockValuationReportDto val = reportingService.getStockValuation();
        TransferReportDto xfer = reportingService.getTransferReport();

        List<SheetData> sheets = new ArrayList<>();

        long completed = (sales != null && sales.ordersByStatus() != null)
                ? sales.ordersByStatus().getOrDefault("DELIVERED", 0L) : 0L;
        long pending = (sales != null && sales.ordersByStatus() != null)
                ? (sales.ordersByStatus().getOrDefault("PENDING_PAYMENT", 0L)
                + sales.ordersByStatus().getOrDefault("PROCESSING", 0L)
                + sales.ordersByStatus().getOrDefault("READY_FOR_DISPATCH", 0L)) : 0L;
        long cancelled = (sales != null && sales.ordersByStatus() != null)
                ? sales.ordersByStatus().getOrDefault("CANCELLED", 0L) : 0L;

        // Sheet 1: Executive KPI Summary
        List<String> summaryHeaders = List.of("Metric Category", "Key Indicator", "Value / Count");
        List<List<Object>> summaryRows = List.of(
                row("Commercial Sales", "Total Realized Revenue (ETB)", sales != null ? sales.totalRevenueEtb() : BigDecimal.ZERO),
                row("Commercial Sales", "Average Order Value (ETB)", sales != null ? sales.averageOrderValueEtb() : BigDecimal.ZERO),
                row("Commercial Sales", "Total Orders Processed", sales != null ? sales.totalOrders() : 0L),
                row("Commercial Sales", "Completed & Delivered Orders", completed),
                row("Commercial Sales", "Pending Fulfillment Orders", pending),
                row("Commercial Sales", "Cancelled / Rejected Orders", cancelled),
                row("Commercial Sales", "Delivery Orders", sales != null ? sales.deliveryOrdersCount() : 0L),
                row("Commercial Sales", "Branch Collection Orders", sales != null ? sales.collectionOrdersCount() : 0L),
                row("Inventory Valuation", "Total Inventory Valuation (ETB)", val != null ? val.totalValuationEtb() : BigDecimal.ZERO),
                row("Inventory Valuation", "Total Catalog Products", val != null ? val.productCount() : (stock != null ? stock.totalProducts() : 0)),
                row("Branch Inventory", "Total Physical Units On Hand", stock != null ? stock.totalUnitsOnHand() : BigDecimal.ZERO),
                row("Branch Inventory", "Operating Branches Count", stock != null && stock.branchSummaries() != null ? stock.branchSummaries().size() : 0),
                row("Logistics & Transfers", "Total Transfer Shipments", xfer != null ? xfer.totalTransfers() : 0L),
                row("Logistics & Transfers", "In-Transit Shipments", xfer != null ? xfer.inTransitCount() : 0L),
                row("Logistics & Transfers", "Safely Received Shipments", xfer != null ? xfer.receivedCount() : 0L),
                row("Logistics & Transfers", "Discrepancy Shipments", xfer != null ? xfer.discrepancyCount() : 0L)
        );
        sheets.add(new SheetData("Executive Summary", summaryHeaders, summaryRows));

        // Sheet 2: Stock Valuation (WAC)
        if (val != null && val.productValuations() != null && !val.productValuations().isEmpty()) {
            List<String> valHeaders = List.of("SKU", "Product Name", "Category", "Warehouse", "Physical Units On Hand", "Unit Cost (WAC ETB)", "Total Valuation (ETB)");
            List<List<Object>> valRows = val.productValuations().stream().map(item -> row(
                    item.sku(),
                    item.productName(),
                    item.categoryName() != null ? item.categoryName() : "General",
                    item.warehouseName(),
                    item.onHandQuantity(),
                    item.weightedAverageCost(),
                    item.totalValuation()
            )).toList();
            sheets.add(new SheetData("Stock Valuation (WAC)", valHeaders, valRows));
        }

        // Sheet 3: Branch Stock Breakdown
        if (stock != null && stock.branchSummaries() != null && !stock.branchSummaries().isEmpty()) {
            List<String> branchHeaders = List.of("Branch Name", "Branch Code", "Warehouse Count", "Total On Hand Units", "Total Reserved Units", "Total Available Units", "Branch Valuation (ETB)");
            List<List<Object>> branchRows = stock.branchSummaries().stream().map(b -> row(
                    b.branchName(),
                    b.branchCode(),
                    b.warehouseCount(),
                    b.totalOnHand(),
                    b.totalReserved(),
                    b.totalAvailable(),
                    b.totalValuationEtb() != null ? b.totalValuationEtb() : BigDecimal.ZERO
            )).toList();
            sheets.add(new SheetData("Branch Stock Summary", branchHeaders, branchRows));
        }

        byte[] bytes = excelExportService.exportMultiSheetExcel(sheets);
        String filename = excelExportService.generateTimestampedFilename("Aser_Commercial_Intelligence_Full_Report");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }
}
