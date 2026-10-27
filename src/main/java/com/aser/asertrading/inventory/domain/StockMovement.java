package com.aser.asertrading.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_movements_product", columnList = "product_id"),
        @Index(name = "idx_movements_warehouse", columnList = "warehouse_id"),
        @Index(name = "idx_movements_type", columnList = "movement_type"),
        @Index(name = "idx_movements_ref", columnList = "reference_type, reference_id"),
        @Index(name = "idx_movements_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "quantity_delta", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30)
    private StockMovementType movementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false, length = 30)
    private ReferenceType referenceType;

    @Column(name = "reference_id", length = 80)
    private String referenceId;

    @Column(name = "unit_cost", precision = 18, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "total_cost", precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Column(length = 100)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

