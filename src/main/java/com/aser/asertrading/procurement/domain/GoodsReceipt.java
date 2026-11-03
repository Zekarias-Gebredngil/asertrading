package com.aser.asertrading.procurement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts", indexes = {
        @Index(name = "idx_grn_number", columnList = "grn_number", unique = true),
        @Index(name = "idx_grn_po", columnList = "purchase_order_id"),
        @Index(name = "idx_grn_warehouse", columnList = "warehouse_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grn_number", nullable = false, unique = true, length = 40)
    private String grnNumber;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "warehouse_id", nullable = false)
    private Long warehouseId;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "delivery_note_number", length = 60)
    private String deliveryNoteNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "goodsReceipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<GoodsReceiptItem> items = new ArrayList<>();

    @Column(name = "received_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();
}

