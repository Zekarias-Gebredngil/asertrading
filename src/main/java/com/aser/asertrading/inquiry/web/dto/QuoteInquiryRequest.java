package com.aser.asertrading.inquiry.web.dto;

import com.aser.asertrading.inquiry.domain.InquiryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record QuoteInquiryRequest(
        @NotNull(message = "Quoted price is required")
        @PositiveOrZero(message = "Quoted price cannot be negative")
        BigDecimal quotedPrice,

        String adminNotes,
        InquiryStatus status
) {}

