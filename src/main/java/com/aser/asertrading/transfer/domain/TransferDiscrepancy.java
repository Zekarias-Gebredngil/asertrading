package com.aser.asertrading.transfer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transfer_discrepancies", indexes = {
        @Index(name = "idx_discrepancies_transfer", columnList = "transfer_id"),
        @Index(name = "idx_discrepancies_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferDiscrepancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "expected_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal expectedQuantity;

    @Column(name = "actual_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal actualQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private boolean isResolved = false;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "reported_by", length = 100)
    private String reportedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

