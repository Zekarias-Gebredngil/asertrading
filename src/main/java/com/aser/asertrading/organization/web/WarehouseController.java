package com.aser.asertrading.organization.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.organization.service.WarehouseService;
import com.aser.asertrading.organization.web.dto.CreateWarehouseRequest;
import com.aser.asertrading.organization.web.dto.WarehouseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouses", description = "Endpoints for managing branch warehouses and stock locations")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @Operation(summary = "Get all warehouses")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER')")
    public ApiResponse<List<WarehouseDto>> getAllWarehouses() {
        return ApiResponse.success(warehouseService.getAllWarehouses());
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Get warehouses by branch ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<List<WarehouseDto>> getWarehousesByBranch(@PathVariable Long branchId) {
        return ApiResponse.success(warehouseService.getWarehousesByBranch(branchId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get warehouse details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<WarehouseDto> getWarehouseById(@PathVariable Long id) {
        return ApiResponse.success(warehouseService.getWarehouseById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new warehouse for a branch")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(@Valid @RequestBody CreateWarehouseRequest request, Authentication auth) {
        WarehouseDto warehouse = warehouseService.createWarehouse(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Warehouse created successfully", warehouse));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update warehouse details")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ApiResponse<WarehouseDto> updateWarehouse(
            @PathVariable Long id,
            @Valid @RequestBody CreateWarehouseRequest request,
            Authentication auth) {

        return ApiResponse.success("Warehouse updated successfully", warehouseService.updateWarehouse(id, request, auth.getName()));
    }
}

