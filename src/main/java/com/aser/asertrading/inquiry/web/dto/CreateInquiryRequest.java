package com.aser.asertrading.inquiry.web.dto;

import com.aser.asertrading.inquiry.domain.InquiryType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateInquiryRequest(
        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Email is required")
        @Email
        String email,

        String phone,
        String companyName,
        String serviceType,
        InquiryType inquiryType,

        @NotBlank(message = "Subject is required")
        @Size(max = 200)
        String subject,

        @NotBlank(message = "Message or project detail is required")
        String message,

        String specifications,
        BigDecimal requestedQuantity
) {}

