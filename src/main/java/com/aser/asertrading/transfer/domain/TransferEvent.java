package com.aser.asertrading.transfer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "transfer_events", indexes = {
        @Index(name = "idx_transfer_events_transfer", columnList = "transfer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private StockTransfer transfer;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType; // CREATED, SUBMITTED, APPROVED, REJECTED, DISPATCHED, RECEIVED, DISCREPANCY, CLOSED

    @Column(length = 100)
    private String actor;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

