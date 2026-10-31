package com.aser.asertrading.transfer.domain;

public enum TransferStatus {
    DRAFT,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    IN_TRANSIT,
    RECEIVED,
    PARTIALLY_RECEIVED,
    DISCREPANCY_REVIEW,
    CLOSED_WITH_VARIANCE,
    CANCELLED
}

