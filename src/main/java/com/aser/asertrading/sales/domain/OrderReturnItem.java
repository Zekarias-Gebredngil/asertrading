package com.aser.asertrading.sales.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_return_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_return_id", nullable = false)
    private OrderReturn orderReturn;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnAction action = ReturnAction.RESTOCK;
}

