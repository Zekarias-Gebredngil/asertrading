package com.aser.asertrading.sales.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    PROCESSING,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED
}

