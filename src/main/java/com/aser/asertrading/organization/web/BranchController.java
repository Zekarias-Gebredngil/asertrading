package com.aser.asertrading.organization.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.organization.service.BranchService;
import com.aser.asertrading.organization.web.dto.BranchDto;
import com.aser.asertrading.organization.web.dto.CreateBranchRequest;
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
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branches", description = "Endpoints for managing business branches")
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @Operation(summary = "Get all company branches")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER', 'SALES_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<List<BranchDto>> getAllBranches() {
        return ApiResponse.success(branchService.getAllBranches());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get branch details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<BranchDto> getBranchById(@PathVariable Long id) {
        return ApiResponse.success(branchService.getBranchById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new branch")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<BranchDto>> createBranch(@Valid @RequestBody CreateBranchRequest request, Authentication auth) {
        BranchDto branch = branchService.createBranch(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Branch created successfully", branch));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update branch details")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    public ApiResponse<BranchDto> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody CreateBranchRequest request,
            Authentication auth) {

        return ApiResponse.success("Branch updated successfully", branchService.updateBranch(id, request, auth.getName()));
    }
}

