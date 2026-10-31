package com.aser.asertrading.transfer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "stock_transfer_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "requested_quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal requestedQuantity;

    @Column(name = "approved_quantity", precision = 18, scale = 4)
    private BigDecimal approvedQuantity;

    @Column(name = "dispatched_quantity", precision = 18, scale = 4)
    private BigDecimal dispatchedQuantity;

    @Column(name = "received_quantity", precision = 18, scale = 4)
    private BigDecimal receivedQuantity;
}

