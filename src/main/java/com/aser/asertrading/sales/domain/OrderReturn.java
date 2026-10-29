package com.aser.asertrading.sales.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "order_returns", indexes = {
        @Index(name = "idx_returns_order", columnList = "order_id"),
        @Index(name = "idx_returns_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReturnStatus status = ReturnStatus.REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "inspected_by", length = 100)
    private String inspectedBy;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "orderReturn", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderReturnItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

