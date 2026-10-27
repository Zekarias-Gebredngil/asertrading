package com.aser.asertrading.inventory.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_count_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockCountItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stock_count_id", nullable = false)
    private StockCount stockCount;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "snapshot_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal snapshotQuantity;

    @Column(name = "counted_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal countedQuantity;

    @Column(name = "variance_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal varianceQuantity;
}

