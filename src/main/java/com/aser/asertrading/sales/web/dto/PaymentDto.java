package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.Payment;
import com.aser.asertrading.sales.domain.PaymentMethod;
import com.aser.asertrading.sales.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
        Long id,
        Long orderId,
        String orderNumber,
        PaymentMethod paymentMethod,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String referenceNumber,
        String confirmedBy,
        Instant confirmedAt,
        String notes,
        Instant createdAt
) {
    public static PaymentDto of(Payment p, String orderNumber) {
        return new PaymentDto(
                p.getId(),
                p.getOrderId(),
                orderNumber,
                p.getPaymentMethod(),
                p.getAmount(),
                p.getCurrency(),
                p.getStatus(),
                p.getReferenceNumber(),
                p.getConfirmedBy(),
                p.getConfirmedAt(),
                p.getNotes(),
                p.getCreatedAt()
        );
    }
}

