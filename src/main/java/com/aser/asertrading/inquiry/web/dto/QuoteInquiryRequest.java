package com.aser.asertrading.inquiry.web.dto;

import com.aser.asertrading.inquiry.domain.InquiryStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record QuoteInquiryRequest(
        @NotNull(message = "Quoted price is required")
        @PositiveOrZero(message = "Quoted price cannot be negative")
        @JsonAlias({"quotedAmount", "amount", "price"})
        BigDecimal quotedPrice,

        @PositiveOrZero(message = "Estimated cost cannot be negative")
        @JsonAlias({"cost", "costPrice", "productionCost"})
        BigDecimal estimatedCost,

        @JsonAlias({"notes", "note", "comment"})
        String adminNotes,

        InquiryStatus status
) {}

