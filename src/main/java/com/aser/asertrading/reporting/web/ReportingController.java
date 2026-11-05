package com.aser.asertrading.reporting.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.identity.service.SecurityBranchService;
import com.aser.asertrading.reporting.dto.SalesReportDto;
import com.aser.asertrading.reporting.dto.StockSummaryReportDto;
import com.aser.asertrading.reporting.dto.StockValuationReportDto;
import com.aser.asertrading.reporting.dto.TransferReportDto;
import com.aser.asertrading.reporting.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports & Analytics", description = "Endpoints for generating stock, sales, valuation and transfer reports")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'ACCOUNTANT_AUDITOR')")
public class ReportingController {

    private final ReportingService reportingService;
    private final SecurityBranchService securityBranchService;

    @GetMapping("/stock-summary")
    @Operation(summary = "Current stock summary aggregated by branch and warehouse")
    public ApiResponse<StockSummaryReportDto> getStockSummary(@RequestParam(required = false) Long branchId) {
        Long effectiveBranchId = securityBranchService.resolveEffectiveBranchId(branchId);
        return ApiResponse.success(reportingService.getStockSummary(effectiveBranchId));
    }

    @GetMapping("/stock-valuation")
    @Operation(summary = "Weighted-average inventory valuation report in Ethiopian Birr (ETB)")
    public ApiResponse<StockValuationReportDto> getStockValuation(@RequestParam(required = false) Long branchId) {
        Long effectiveBranchId = securityBranchService.resolveEffectiveBranchId(branchId);
        return ApiResponse.success(reportingService.getStockValuation(effectiveBranchId));
    }

    @GetMapping("/sales")
    @Operation(summary = "Sales analytics, revenue, order fulfillment status and averages")
    public ApiResponse<SalesReportDto> getSalesReport(@RequestParam(required = false) Long branchId) {
        Long effectiveBranchId = securityBranchService.resolveEffectiveBranchId(branchId);
        return ApiResponse.success(reportingService.getSalesReport(effectiveBranchId));
    }

    @GetMapping("/transfer-history")
    @Operation(summary = "Inter-branch transfer summary and discrepancy report")
    public ApiResponse<TransferReportDto> getTransferReport(@RequestParam(required = false) Long branchId) {
        Long effectiveBranchId = securityBranchService.resolveEffectiveBranchId(branchId);
        return ApiResponse.success(reportingService.getTransferReport(effectiveBranchId));
    }
}
