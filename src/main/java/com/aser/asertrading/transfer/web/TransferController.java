package com.aser.asertrading.transfer.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.transfer.domain.TransferStatus;
import com.aser.asertrading.transfer.service.TransferWorkflowService;
import com.aser.asertrading.transfer.web.dto.*;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Tag(name = "Inter-Branch Transfers", description = "Endpoints for managing inter-warehouse stock transfer lifecycle and approvals")
public class TransferController {

    private final TransferWorkflowService transferWorkflowService;

    @PostMapping
    @Operation(summary = "Create an inter-branch transfer request in DRAFT status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ResponseEntity<ApiResponse<StockTransferDto>> createTransfer(@Valid @RequestBody CreateTransferRequest request, Authentication auth) {
        StockTransferDto transfer = transferWorkflowService.createTransfer(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Transfer created in DRAFT status", transfer));
    }

    @GetMapping
    @Operation(summary = "List authorized stock transfers with optional status filter")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<StockTransferDto>> getTransfers(
            @RequestParam(required = false) TransferStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(transferWorkflowService.getTransfers(status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<StockTransferDto> getTransferById(@PathVariable Long id) {
        return ApiResponse.success(transferWorkflowService.getTransferById(id));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit a draft transfer request for management approval")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<StockTransferDto> submitTransfer(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Transfer submitted for approval", transferWorkflowService.submitTransfer(id, auth.getName()));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve transfer request (enforces separation of duties: requester cannot approve)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER')")
    public ApiResponse<StockTransferDto> approveTransfer(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Transfer approved successfully", transferWorkflowService.approveTransfer(id, auth.getName()));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject transfer request with explanation reason")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER')")
    public ApiResponse<StockTransferDto> rejectTransfer(
            @PathVariable Long id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body,
            Authentication auth) {

        String effectiveReason = (reason != null && !reason.isBlank()) ? reason :
                (body != null && body.containsKey("reason")) ? body.get("reason") : "Rejected by management";

        return ApiResponse.success("Transfer rejected", transferWorkflowService.rejectTransfer(id, effectiveReason, auth.getName()));
    }

    @PostMapping("/{id}/dispatch")
    @Operation(summary = "Dispatch transfer goods (deducts source warehouse stock balance and marks IN_TRANSIT)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER', 'BRANCH_MANAGER')")
    public ApiResponse<StockTransferDto> dispatchTransfer(@PathVariable Long id, Authentication auth) {
        return ApiResponse.success("Goods dispatched; now in transit", transferWorkflowService.dispatchTransfer(id, auth.getName()));
    }

    @PostMapping("/{id}/receive")
    @Operation(summary = "Confirm destination receipt of goods (increases destination stock, handles discrepancies)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER', 'BRANCH_MANAGER')")
    public ApiResponse<StockTransferDto> receiveTransfer(
            @PathVariable Long id,
            @Valid @RequestBody TransferReceiveRequest request,
            Authentication auth) {

        return ApiResponse.success("Transfer receipt recorded", transferWorkflowService.receiveTransfer(id, request, auth.getName()));
    }

    @PostMapping("/{id}/report-discrepancy")
    @Operation(summary = "Report discrepancy (shortage, damage, excess) on a transfer")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER', 'BRANCH_MANAGER')")
    public ApiResponse<TransferDiscrepancyDto> reportDiscrepancy(
            @PathVariable Long id,
            @Valid @RequestBody ReportDiscrepancyRequest request,
            Authentication auth) {

        return ApiResponse.success("Discrepancy recorded", transferWorkflowService.reportDiscrepancy(id, request, auth.getName()));
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get audit event history for a transfer")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<List<TransferEventDto>> getTransferEvents(@PathVariable Long id) {
        return ApiResponse.success(transferWorkflowService.getTransferEvents(id));
    }

    @GetMapping("/{id}/discrepancies")
    @Operation(summary = "Get discrepancy reports for a transfer")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER')")
    public ApiResponse<List<TransferDiscrepancyDto>> getDiscrepancies(@PathVariable Long id) {
        return ApiResponse.success(transferWorkflowService.getDiscrepancies(id));
    }
}

