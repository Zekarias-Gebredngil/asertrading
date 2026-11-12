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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/export")
@RequiredArgsConstructor
@Tag(name = "Data Export Engine", description = "Endpoints for downloading Excel (.xlsx) reports across all system contents or requested pages")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR', 'SALES_OFFICER')")
// Data export REST controller test
public class ExportController {

    private final ExcelExportService excelExportService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final ProductService productService;
    private final TransferWorkflowService transferWorkflowService;
    private final AuditService auditService;
    private final ReportingService reportingService;

    private static final MediaType EXCEL_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private static final List<String> INVENTORY_HEADERS = List.of(
            "SKU", "Product Name", "Category", "Branch", "Warehouse",
            "Physical On Hand", "Reserved Qty", "Available to Sell", "Unit",
            "Weighted Avg Cost (ETB)", "Total Valuation (ETB)", "Status"
    );

    private static final List<String> MOVEMENT_HEADERS = List.of(
            "Timestamp", "Movement Type", "Product SKU", "Product Name", "Branch", "Warehouse",
            "Quantity Delta", "Unit", "Unit Cost (ETB)", "Total Cost (ETB)", "Reference Type", "Reference ID", "Actor", "Notes"
    );

    private static final List<String> ORDER_HEADERS = List.of(
            "Order Number", "Date Created", "Customer Name", "Customer Phone", "Customer Email",
            "Fulfillment Type", "Branch / Collection", "Delivery Address", "Status",
            "Subtotal (ETB)", "Delivery Fee (ETB)", "Total Amount (ETB)", "Payment Method", "Payment Status"
    );

    private static final List<String> PRODUCT_HEADERS = List.of(
            "SKU", "Product Name", "Category", "Unit", "Base Cost (ETB)", "Selling Price (ETB)",
            "Pricing Model", "Availability", "Reorder Threshold", "Total Stock On Hand", "Published", "Status"
    );

    private static final List<String> TRANSFER_HEADERS = List.of(
            "Transfer #", "Source Warehouse", "Destination Warehouse", "Status",
            "Items Count", "Requested By", "Approved By", "Dispatched By", "Received By", "Notes"
    );

    private static final List<String> AUDIT_HEADERS = List.of(
            "Record ID", "Timestamp", "Actor", "Action", "Entity Module", "Entity ID", "Forensic Details", "IP Address"
    );

    private static final List<String> SUMMARY_HEADERS = List.of("Metric Category", "Key Indicator", "Value / Count");

    private static final List<String> VALUATION_HEADERS = List.of(
            "SKU", "Product Name", "Category", "Warehouse", "Physical Units On Hand", "Unit Cost (WAC ETB)", "Total Valuation (ETB)"
    );

    private static final List<String> BRANCH_HEADERS = List.of(
            "Branch Name", "Branch Code", "Warehouse Count", "Total On Hand Units", "Total Reserved Units", "Total Available Units", "Branch Valuation (ETB)"
    );

    private List<Object> row(Object... items) {
        return Arrays.stream(items)
                .map(item -> Objects.requireNonNullElse(item, ""))
                .toList();
    }

    private Pageable createExportPageable(boolean exportAll, Pageable pageable) {
        return exportAll ? Pageable.unpaged() : pageable;
    }

    private String resolveExportFilename(String baseName, boolean exportAll, int page) {
        String suffix = exportAll ? "_All" : "_Page_" + (page + 1);
        return excelExportService.generateTimestampedFilename(baseName + suffix);
    }

    private ResponseEntity<byte[]> createExcelResponse(byte[] bytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(EXCEL_MEDIA_TYPE)
                .body(bytes);
    }

    private String resolveBaseUnit(ProductDto product) {
        if (product.baseUnitSymbol() != null) {
            return product.baseUnitSymbol();
        }
        if (product.baseUnitName() != null) {
            return product.baseUnitName();
        }
        return "piece";
    }

    private String enumName(Enum<?> e) {
        return e != null ? e.name() : "";
    }

    private int resolveCatalogProductCount(StockValuationReportDto val, StockSummaryReportDto stock) {
        if (val != null) {
            return val.productCount();
        }
        return stock != null ? stock.totalProducts() : 0;
    }



    @GetMapping("/movements")
    @Operation(summary = "Export stock movements to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportMovements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) StockMovementType movementType,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable effectivePageable = createExportPageable(exportAll, pageable);
        Page<StockMovementDto> movementsPage = inventoryService.getMovements(productId, warehouseId, movementType, effectivePageable);

        List<List<Object>> rows = movementsPage.getContent().stream().map(m -> row(
                m.createdAt(),
                enumName(m.movementType()),
                m.productSku(),
                m.productName(),
                m.branchName(),
                m.warehouseName(),
                m.quantityDelta(),
                m.unitSymbol(),
                m.unitCost(),
                m.totalCost(),
                enumName(m.referenceType()),
                m.referenceId(),
                m.actor(),
                m.notes()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Stock Movements", MOVEMENT_HEADERS, rows);
        String filename = resolveExportFilename("Aser_Stock_Movements", exportAll, pageable.getPageNumber());

        return createExcelResponse(bytes, filename);
    }

    @GetMapping("/orders")
    @Operation(summary = "Export orders to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable effectivePageable = createExportPageable(exportAll, pageable);
        Page<OrderDto> ordersPage = orderService.getOrders(status, branchId, effectivePageable);

        List<List<Object>> rows = ordersPage.getContent().stream().map(o -> row(
                o.orderNumber(),
                o.createdAt(),
                o.customerName(),
                o.customerPhone(),
                o.customerEmail(),
                enumName(o.deliveryType()),
                o.collectionBranchName(),
                o.deliveryAddress(),
                enumName(o.status()),
                o.subtotal(),
                o.deliveryFee(),
                o.totalAmount(),
                o.paymentMethod(),
                o.paymentStatus()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Customer Orders", ORDER_HEADERS, rows);
        String filename = resolveExportFilename("Aser_Orders", exportAll, pageable.getPageNumber());

        return createExcelResponse(bytes, filename);
    }

    @GetMapping("/products")
    @Operation(summary = "Export product catalog to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean quotationOnly,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        Pageable effectivePageable = createExportPageable(exportAll, pageable);
        Page<ProductDto> productsPage = productService.searchProducts(status, categoryId, quotationOnly, search, effectivePageable);

        List<List<Object>> rows = productsPage.getContent().stream().map(p -> row(
                p.sku(),
                p.name(),
                p.categoryName(),
                resolveBaseUnit(p),
                p.purchaseCost(),
                p.sellingPrice(),
                p.isQuotationOnly() ? "Quotation" : "Fixed",
                enumName(p.availability()),
                p.reorderThreshold(),
                p.totalStockOnHand(),
                p.isPublished() ? "Published" : "Draft",
                enumName(p.status())
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Products Catalog", PRODUCT_HEADERS, rows);
        String filename = resolveExportFilename("Aser_Products_Catalog", exportAll, pageable.getPageNumber());

        return createExcelResponse(bytes, filename);
    }

    @GetMapping("/transfers")
    @Operation(summary = "Export inter-branch transfers to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportTransfers(
            @RequestParam(required = false) TransferStatus status,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable effectivePageable = createExportPageable(exportAll, pageable);
        Page<StockTransferDto> transfersPage = transferWorkflowService.getTransfers(status, effectivePageable);

        List<List<Object>> rows = transfersPage.getContent().stream().map(t -> row(
                t.transferNumber(),
                t.sourceWarehouseName(),
                t.destinationWarehouseName(),
                enumName(t.status()),
                t.items() != null ? t.items().size() : 0,
                t.requestedBy(),
                t.approvedBy(),
                t.dispatchedBy(),
                t.receivedBy(),
                t.notes()
        )).toList();

        byte[] bytes = excelExportService.exportToExcel("Branch Transfers", TRANSFER_HEADERS, rows);
        String filename = resolveExportFilename("Aser_Stock_Transfers", exportAll, pageable.getPageNumber());

        return createExcelResponse(bytes, filename);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Export audit logs to Excel (all records or requested page)")
    public ResponseEntity<byte[]> exportAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(defaultValue = "true") boolean exportAll,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Pageable effectivePageable = createExportPageable(exportAll, pageable);
        Page<AuditLog> logsPage = StringUtils.hasText(entityType)
                ? auditService.getLogsByEntity(entityType, effectivePageable)
                : auditService.getLogs(effectivePageable);

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

        byte[] bytes = excelExportService.exportToExcel("System Audit Trail", AUDIT_HEADERS, rows);
        String filename = resolveExportFilename("Aser_Audit_Logs", exportAll, pageable.getPageNumber());

        return createExcelResponse(bytes, filename);
    }

    @GetMapping("/reports")
    @Operation(summary = "Export full commercial intelligence multi-sheet report to Excel")
    public ResponseEntity<byte[]> exportFullCommercialReport() {
        SalesReportDto sales = reportingService.getSalesReport();
        StockSummaryReportDto stock = reportingService.getStockSummary();
        StockValuationReportDto val = reportingService.getStockValuation();
        TransferReportDto xfer = reportingService.getTransferReport();

        List<SheetData> sheets = new ArrayList<>();
        sheets.add(buildExecutiveSummarySheet(sales, stock, val, xfer));

        if (val != null && val.productValuations() != null && !val.productValuations().isEmpty()) {
            sheets.add(buildStockValuationSheet(val));
        }

        if (stock != null && stock.branchSummaries() != null && !stock.branchSummaries().isEmpty()) {
            sheets.add(buildBranchStockSheet(stock));
        }

        byte[] bytes = excelExportService.exportMultiSheetExcel(sheets);
        String filename = excelExportService.generateTimestampedFilename("Aser_Commercial_Intelligence_Full_Report");

        return createExcelResponse(bytes, filename);
    }

    private SheetData buildExecutiveSummarySheet(
            SalesReportDto sales,
            StockSummaryReportDto stock,
            StockValuationReportDto val,
            TransferReportDto xfer) {

        Map<String, Long> statusMap = (sales != null && sales.ordersByStatus() != null)
                ? sales.ordersByStatus()
                : Map.of();

        long completed = statusMap.getOrDefault("DELIVERED", 0L);
        long pending = statusMap.getOrDefault("PENDING_PAYMENT", 0L)
                + statusMap.getOrDefault("PROCESSING", 0L)
                + statusMap.getOrDefault("READY_FOR_DISPATCH", 0L);
        long cancelled = statusMap.getOrDefault("CANCELLED", 0L);

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
                row("Inventory Valuation", "Total Catalog Products", resolveCatalogProductCount(val, stock)),
                row("Branch Inventory", "Total Physical Units On Hand", stock != null ? stock.totalUnitsOnHand() : BigDecimal.ZERO),
                row("Branch Inventory", "Operating Branches Count", stock != null && stock.branchSummaries() != null ? stock.branchSummaries().size() : 0),
                row("Logistics & Transfers", "Total Transfer Shipments", xfer != null ? xfer.totalTransfers() : 0L),
                row("Logistics & Transfers", "In-Transit Shipments", xfer != null ? xfer.inTransitCount() : 0L),
                row("Logistics & Transfers", "Safely Received Shipments", xfer != null ? xfer.receivedCount() : 0L),
                row("Logistics & Transfers", "Discrepancy Shipments", xfer != null ? xfer.discrepancyCount() : 0L)
        );

        return new SheetData("Executive Summary", SUMMARY_HEADERS, summaryRows);
    }

    private SheetData buildStockValuationSheet(StockValuationReportDto val) {
        List<List<Object>> valRows = val.productValuations().stream().map(item -> row(
                item.sku(),
                item.productName(),
                Objects.requireNonNullElse(item.categoryName(), "General"),
                item.warehouseName(),
                item.onHandQuantity(),
                item.weightedAverageCost(),
                item.totalValuation()
        )).toList();

        return new SheetData("Stock Valuation (WAC)", VALUATION_HEADERS, valRows);
    }

    private SheetData buildBranchStockSheet(StockSummaryReportDto stock) {
        List<List<Object>> branchRows = stock.branchSummaries().stream().map(b -> row(
                b.branchName(),
                b.branchCode(),
                b.warehouseCount(),
                b.totalOnHand(),
                b.totalReserved(),
                b.totalAvailable(),
                Objects.requireNonNullElse(b.totalValuationEtb(), BigDecimal.ZERO)
        )).toList();

        return new SheetData("Branch Stock Summary", BRANCH_HEADERS, branchRows);
    }
}
