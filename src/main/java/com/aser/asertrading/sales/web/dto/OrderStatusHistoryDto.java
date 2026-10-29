package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.OrderStatus;
import com.aser.asertrading.sales.domain.OrderStatusHistory;

import java.time.Instant;

public record OrderStatusHistoryDto(
        Long id,
        OrderStatus status,
        String actor,
        String comment,
        Instant createdAt
) {
    public static OrderStatusHistoryDto fromEntity(OrderStatusHistory h) {
        return new OrderStatusHistoryDto(
                h.getId(),
                h.getStatus(),
                h.getActor(),
                h.getComment(),
                h.getCreatedAt()
        );
    }
}

