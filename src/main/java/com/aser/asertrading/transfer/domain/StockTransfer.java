package com.aser.asertrading.transfer.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stock_transfers", indexes = {
        @Index(name = "idx_transfers_number", columnList = "transfer_number", unique = true),
        @Index(name = "idx_transfers_source", columnList = "source_warehouse_id"),
        @Index(name = "idx_transfers_destination", columnList = "destination_warehouse_id"),
        @Index(name = "idx_transfers_status", columnList = "status"),
        @Index(name = "idx_transfers_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 40)
    private String transferNumber;

    @Column(name = "source_warehouse_id", nullable = false)
    private Long sourceWarehouseId;

    @Column(name = "destination_warehouse_id", nullable = false)
    private Long destinationWarehouseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(name = "requested_by", length = 100)
    private String requestedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "dispatched_by", length = 100)
    private String dispatchedBy;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "rejection_reason", length = 300)
    private String rejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StockTransferItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TransferEvent> events = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

