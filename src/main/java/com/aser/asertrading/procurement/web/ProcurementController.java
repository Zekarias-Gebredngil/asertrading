package com.aser.asertrading.procurement.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.procurement.domain.POStatus;
import com.aser.asertrading.procurement.service.ProcurementService;
import com.aser.asertrading.procurement.web.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Procurement & Suppliers", description = "Endpoints for managing vendors, purchase orders, and goods receiving")
public class ProcurementController {

    private final ProcurementService procurementService;

    // Supplier Endpoints
    @GetMapping("/suppliers")
    @Operation(summary = "List suppliers with strict pagination")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'PROCUREMENT_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<SupplierDto>> getSuppliers(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(PagedResponse.of(procurementService.getSuppliers(pageable)));
    }

    @PostMapping("/suppliers")
    @Operation(summary = "Create a new supplier")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<SupplierDto>> createSupplier(@Valid @RequestBody CreateSupplierRequest request, Authentication auth) {
        SupplierDto supplier = procurementService.createSupplier(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Supplier created successfully", supplier));
    }

    // Purchase Order Endpoints
    @GetMapping("/purchase-orders")
    @Operation(summary = "List purchase orders with optional status filter")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'PROCUREMENT_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<PurchaseOrderDto>> getPurchaseOrders(
            @RequestParam(required = false) POStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(procurementService.getPurchaseOrders(status, pageable)));
    }

    @GetMapping("/purchase-orders/{id}")
    @Operation(summary = "Get purchase order details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'PROCUREMENT_OFFICER', 'ACCOUNTANT_AUDITOR', 'STOREKEEPER')")
    public ApiResponse<PurchaseOrderDto> getPurchaseOrderById(@PathVariable Long id) {
        return ApiResponse.success(procurementService.getPurchaseOrderById(id));
    }

    @PostMapping("/purchase-orders")
    @Operation(summary = "Create a new purchase order in DRAFT status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'PROCUREMENT_OFFICER')")
    public ResponseEntity<ApiResponse<PurchaseOrderDto>> createPurchaseOrder(@Valid @RequestBody CreatePORequest request, Authentication auth) {
        PurchaseOrderDto po = procurementService.createPurchaseOrder(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Purchase order created", po));
    }

    @PostMapping("/purchase-orders/{id}/approve")
    @Operation(summary = "Approve purchase order")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER')")
    public ApiResponse<PurchaseOrderDto> approvePurchaseOrder(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Purchase order approved", procurementService.approvePurchaseOrder(id, auth.getName()));
    }

    @PostMapping("/purchase-orders/{id}/receipts")
    @Operation(summary = "Record goods delivery from supplier against purchase order (posts stock receipt & updates weighted average cost)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'STOREKEEPER', 'PROCUREMENT_OFFICER')")
    public ApiResponse<GoodsReceiptDto> recordGoodsReceipt(
            @PathVariable Long id,
            @Valid @RequestBody GoodsReceiptRequest request,
            Authentication auth) {

        GoodsReceiptDto grn = procurementService.recordGoodsReceipt(id, request, auth.getName());
        return ApiResponse.success("Goods receipt recorded and inventory balance updated successfully", grn);
    }
}

