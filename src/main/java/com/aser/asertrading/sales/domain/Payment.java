package com.aser.asertrading.sales.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_order", columnList = "order_id"),
        @Index(name = "idx_payments_status", columnList = "status"),
        @Index(name = "idx_payments_reference", columnList = "reference_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "ETB";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "confirmed_by", length = 100)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

