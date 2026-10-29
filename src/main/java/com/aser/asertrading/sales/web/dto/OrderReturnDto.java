package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.OrderReturn;
import com.aser.asertrading.sales.domain.ReturnStatus;

import java.time.Instant;
import java.util.List;

public record OrderReturnDto(
        Long id,
        Long orderId,
        String orderNumber,
        ReturnStatus status,
        String reason,
        String inspectedBy,
        String notes,
        List<OrderReturnItemDto> items,
        Instant createdAt
) {
    public static OrderReturnDto of(OrderReturn r, String orderNumber, List<OrderReturnItemDto> items) {
        return new OrderReturnDto(
                r.getId(),
                r.getOrderId(),
                orderNumber,
                r.getStatus(),
                r.getReason(),
                r.getInspectedBy(),
                r.getNotes(),
                items,
                r.getCreatedAt()
        );
    }
}

