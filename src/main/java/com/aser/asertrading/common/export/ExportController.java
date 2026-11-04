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
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }

    @GetMapping("/orders")
    @Operation(summary = "Export orders to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = exportAll ? Pageable.unpaged() : PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderDto> ordersPage = orderService.getOrders(status, pageable);

        List<String> headers = List.of(
                "Order Number", "Date Created", "Customer Name", "Customer Phone", "Customer Email",
                "Fulfillment Type", "Branch / Collection", "Delivery Address", "Status",
                "Subtotal (ETB)", "Delivery Fee (ETB)", "Total Amount (ETB)", "Payment Method", "Payment Status"
        );

        List<List<Object>> rows = ordersPage.getContent().stream().map(o -> List.<Object>of(
                o.orderNumber() != null ? o.orderNumber() : "",
                o.createdAt() != null ? o.createdAt().toString() : "",
                o.customerName() != null ? o.customerName() : "",
                o.customerPhone() != null ? o.customerPhone() : "",
                o.customerEmail() != null ? o.customerEmail() : "",
                o.deliveryType() != null ? o.deliveryType().name() : "",
                o.collectionBranchName() != null ? o.collectionBranchName() : "",
                o.deliveryAddress() != null ? o.deliveryAddress() : "",
                o.status() != null ? o.status().name() : "",
                o.subtotal() != null ? o.subtotal() : BigDecimal.ZERO,
                o.deliveryFee() != null ? o.deliveryFee() : BigDecimal.ZERO,
                o.totalAmount() != null ? o.totalAmount() : BigDecimal.ZERO,
                o.paymentMethod() != null ? o.paymentMethod() : "",
                o.paymentStatus() != null ? o.paymentStatus() : ""
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

        List<List<Object>> rows = productsPage.getContent().stream().map(p -> List.<Object>of(
                p.sku() != null ? p.sku() : "",
                p.name() != null ? p.name() : "",
                p.categoryName() != null ? p.categoryName() : "",
                p.baseUnitSymbol() != null ? p.baseUnitSymbol() : (p.baseUnitName() != null ? p.baseUnitName() : "piece"),
                p.purchaseCost() != null ? p.purchaseCost() : BigDecimal.ZERO,
                p.sellingPrice() != null ? p.sellingPrice() : BigDecimal.ZERO,
                p.isQuotationOnly() ? "Quotation" : "Fixed",
                p.availability() != null ? p.availability().name() : "",
                p.reorderThreshold() != null ? p.reorderThreshold() : BigDecimal.ZERO,
                p.totalStockOnHand() != null ? p.totalStockOnHand() : BigDecimal.ZERO,
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

        List<List<Object>> rows = transfersPage.getContent().stream().map(t -> List.<Object>of(
                t.transferNumber() != null ? t.transferNumber() : "",
                t.sourceWarehouseName() != null ? t.sourceWarehouseName() : "",
                t.destinationWarehouseName() != null ? t.destinationWarehouseName() : "",
                t.status() != null ? t.status().name() : "",
                t.items() != null ? t.items().size() : 0,
                t.requestedBy() != null ? t.requestedBy() : "",
                t.approvedBy() != null ? t.approvedBy() : "",
                t.dispatchedBy() != null ? t.dispatchedBy() : "",
                t.receivedBy() != null ? t.receivedBy() : "",
                t.notes() != null ? t.notes() : ""
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

        List<List<Object>> rows = logsPage.getContent().stream().map(l -> List.<Object>of(
                l.getId() != null ? l.getId() : 0L,
                l.getCreatedAt() != null ? l.getCreatedAt().toString() : "",
                l.getActor() != null ? l.getActor() : "",
                l.getAction() != null ? l.getAction() : "",
                l.getEntityType() != null ? l.getEntityType() : "",
                l.getEntityId() != null ? l.getEntityId() : "",
                l.getDetails() != null ? l.getDetails() : "",
                l.getIpAddress() != null ? l.getIpAddress() : ""
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

        List<ExcelExportService.SheetData> sheets = new ArrayList<>();

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
                List.of("Commercial Sales", "Total Realized Revenue (ETB)", sales != null ? sales.totalRevenueEtb() : BigDecimal.ZERO),
                List.of("Commercial Sales", "Average Order Value (ETB)", sales != null ? sales.averageOrderValueEtb() : BigDecimal.ZERO),
                List.of("Commercial Sales", "Total Orders Processed", sales != null ? sales.totalOrders() : 0L),
                List.of("Commercial Sales", "Completed & Delivered Orders", completed),
                List.of("Commercial Sales", "Pending Fulfillment Orders", pending),
                List.of("Commercial Sales", "Cancelled / Rejected Orders", cancelled),
                List.of("Commercial Sales", "Delivery Orders", sales != null ? sales.deliveryOrdersCount() : 0L),
                List.of("Commercial Sales", "Branch Collection Orders", sales != null ? sales.collectionOrdersCount() : 0L),
                List.of("Inventory Valuation", "Total Inventory Valuation (ETB)", val != null ? val.totalValuationEtb() : BigDecimal.ZERO),
                List.of("Inventory Valuation", "Total Catalog Products", val != null ? val.productCount() : (stock != null ? stock.totalProducts() : 0)),
                List.of("Branch Inventory", "Total Physical Units On Hand", stock != null ? stock.totalUnitsOnHand() : BigDecimal.ZERO),
                List.of("Branch Inventory", "Operating Branches Count", stock != null && stock.branchSummaries() != null ? stock.branchSummaries().size() : 0),
                List.of("Logistics & Transfers", "Total Transfer Shipments", xfer != null ? xfer.totalTransfers() : 0L),
                List.of("Logistics & Transfers", "In-Transit Shipments", xfer != null ? xfer.inTransitCount() : 0L),
                List.of("Logistics & Transfers", "Safely Received Shipments", xfer != null ? xfer.receivedCount() : 0L),
                List.of("Logistics & Transfers", "Discrepancy Shipments", xfer != null ? xfer.discrepancyCount() : 0L)
        );
        sheets.add(new ExcelExportService.SheetData("Executive Summary", summaryHeaders, summaryRows));

        // Sheet 2: Stock Valuation (WAC)
        if (val != null && val.productValuations() != null && !val.productValuations().isEmpty()) {
            List<String> valHeaders = List.of("SKU", "Product Name", "Category", "Warehouse", "Physical Units On Hand", "Unit Cost (WAC ETB)", "Total Valuation (ETB)");
            List<List<Object>> valRows = val.productValuations().stream().map(item -> List.<Object>of(
                    item.sku() != null ? item.sku() : "",
                    item.productName() != null ? item.productName() : "",
                    item.categoryName() != null ? item.categoryName() : "General",
                    item.warehouseName() != null ? item.warehouseName() : "",
                    item.onHandQuantity() != null ? item.onHandQuantity() : BigDecimal.ZERO,
                    item.weightedAverageCost() != null ? item.weightedAverageCost() : BigDecimal.ZERO,
                    item.totalValuation() != null ? item.totalValuation() : BigDecimal.ZERO
            )).toList();
            sheets.add(new ExcelExportService.SheetData("Stock Valuation (WAC)", valHeaders, valRows));
        }

        // Sheet 3: Branch Stock Breakdown
        if (stock != null && stock.branchSummaries() != null && !stock.branchSummaries().isEmpty()) {
            List<String> branchHeaders = List.of("Branch Name", "Branch Code", "Warehouse Count", "Total On Hand Units", "Total Reserved Units", "Total Available Units");
            List<List<Object>> branchRows = stock.branchSummaries().stream().map(b -> List.<Object>of(
                    b.branchName() != null ? b.branchName() : "",
                    b.branchCode() != null ? b.branchCode() : "",
                    b.warehouseCount(),
                    b.totalOnHand() != null ? b.totalOnHand() : BigDecimal.ZERO,
                    b.totalReserved() != null ? b.totalReserved() : BigDecimal.ZERO,
                    b.totalAvailable() != null ? b.totalAvailable() : BigDecimal.ZERO
            )).toList();
            sheets.add(new ExcelExportService.SheetData("Branch Stock Summary", branchHeaders, branchRows));
        }

        byte[] bytes = excelExportService.exportMultiSheetExcel(sheets);
        String filename = excelExportService.generateTimestampedFilename("Aser_Commercial_Intelligence_Full_Report");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(EXCEL_MEDIA_TYPE))
                .body(bytes);
    }
}
