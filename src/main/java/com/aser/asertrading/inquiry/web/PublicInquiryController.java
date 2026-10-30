package com.aser.asertrading.inquiry.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.inquiry.service.InquiryService;
import com.aser.asertrading.inquiry.web.dto.CreateInquiryRequest;
import com.aser.asertrading.inquiry.web.dto.CustomerInquiryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/inquiries")
@RequiredArgsConstructor
@Tag(name = "Customer Inquiries", description = "Public endpoints for submitting service inquiries and custom quotation requests")
public class PublicInquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @Operation(summary = "Submit customer inquiry or custom quotation request")
    public ResponseEntity<ApiResponse<CustomerInquiryDto>> submitInquiry(
            @Valid @RequestBody CreateInquiryRequest request,
            HttpServletRequest httpRequest) {

        CustomerInquiryDto inquiry = inquiryService.submitInquiry(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inquiry submitted successfully. Your reference: " + inquiry.inquiryNumber(), inquiry));
    }
}

