package com.aser.asertrading.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "stock_balances", uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_warehouse", columnNames = {"product_id", "warehouse_id"})
}, indexes = {
        @Index(name = "idx_stock_product", columnList = "product_id"),
        @Index(name = "idx_stock_warehouse", columnList = "warehouse_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "on_hand_quantity", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal onHandQuantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Column(name = "weighted_average_cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal weightedAverageCost = BigDecimal.ZERO;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public BigDecimal getAvailableQuantity() {
        BigDecimal onHand = this.onHandQuantity != null ? this.onHandQuantity : BigDecimal.ZERO;
        BigDecimal reserved = this.reservedQuantity != null ? this.reservedQuantity : BigDecimal.ZERO;
        BigDecimal available = onHand.subtract(reserved);
        return available.compareTo(BigDecimal.ZERO) > 0 ? available : BigDecimal.ZERO;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

