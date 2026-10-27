package com.aser.asertrading.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "inventory_adjustments", indexes = {
        @Index(name = "idx_adjustments_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_adjustments_product", columnList = "product_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity_delta", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDelta;

    @Column(name = "reason_code", nullable = false, length = 60)
    private String reasonCode; // DAMAGE, EXPIRY, CORRECTION, THEFT, FOUND_STOCK

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

