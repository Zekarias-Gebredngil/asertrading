package com.aser.asertrading.sales.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.sales.domain.ReturnStatus;
import com.aser.asertrading.sales.service.ReturnService;
import com.aser.asertrading.sales.web.dto.ApproveReturnRequest;
import com.aser.asertrading.sales.web.dto.CreateReturnRequest;
import com.aser.asertrading.sales.web.dto.OrderReturnDto;
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

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Order Returns & Refunds", description = "Endpoints for customer return requests, inspection, and restock")
public class ReturnController {

    private final ReturnService returnService;

    @PostMapping("/orders/{orderId}/returns")
    @Operation(summary = "Request return for delivered order items")
    public ResponseEntity<ApiResponse<OrderReturnDto>> requestReturn(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateReturnRequest request,
            Authentication auth) {

        String actor = auth != null ? auth.getName() : "CUSTOMER";
        OrderReturnDto returnDto = returnService.requestReturn(orderId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Return request submitted", returnDto));
    }

    @GetMapping("/returns")
    @Operation(summary = "List return requests")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'SALES_OFFICER', 'STOREKEEPER')")
    public ApiResponse<PagedResponse<OrderReturnDto>> getReturns(
            @RequestParam(required = false) ReturnStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(returnService.getReturns(status, pageable)));
    }

    @GetMapping("/returns/{id}")
    @Operation(summary = "Get return details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'SALES_OFFICER', 'STOREKEEPER')")
    public ApiResponse<OrderReturnDto> getReturnById(@PathVariable Long id) {
        return ApiResponse.success(returnService.getReturnById(id));
    }

    @PostMapping("/returns/{id}/approve")
    @Operation(summary = "Approve return request and restock inspected items atomically")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'STOREKEEPER')")
    public ApiResponse<OrderReturnDto> approveReturn(
            @PathVariable Long id,
            @Valid @RequestBody ApproveReturnRequest request,
            Authentication auth) {

        OrderReturnDto returnDto = returnService.approveReturn(id, request, auth.getName());
        return ApiResponse.success("Return approved and items restocked successfully", returnDto);
    }
}

