package com.aser.asertrading.inquiry.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.inquiry.domain.InquiryStatus;
import com.aser.asertrading.inquiry.domain.InquiryType;
import com.aser.asertrading.inquiry.service.InquiryService;
import com.aser.asertrading.inquiry.web.dto.CustomerInquiryDto;
import com.aser.asertrading.inquiry.web.dto.QuoteInquiryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
@Tag(name = "Customer Inquiries Administration", description = "Endpoints for staff to review inquiries and send custom price quotations")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'SALES_OFFICER', 'WEBSITE_MANAGER')")
public class AdminInquiryController {

    private final InquiryService inquiryService;

    @GetMapping
    @Operation(summary = "List customer inquiries with optional status or type filter")
    public ApiResponse<PagedResponse<CustomerInquiryDto>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryType type,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(inquiryService.getInquiries(status, type, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inquiry details by ID")
    public ApiResponse<CustomerInquiryDto> getInquiryById(@PathVariable Long id) {
        return ApiResponse.success(inquiryService.getInquiryById(id));
    }

    @PostMapping("/{id}/quote")
    @Operation(summary = "Provide a price quotation and notes for a customer inquiry")
    public ApiResponse<CustomerInquiryDto> quoteInquiry(
            @PathVariable Long id,
            @Valid @RequestBody QuoteInquiryRequest request,
            Authentication auth) {

        return ApiResponse.success("Quotation saved", inquiryService.quoteInquiry(id, request, auth.getName()));
    }
}

