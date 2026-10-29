package com.aser.asertrading.sales.web.dto;

import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String orderNumber,
        Long customerId,
        String customerName,
        String customerEmail,
        String customerPhone,
        DeliveryType deliveryType,
        String deliveryAddress,
        Long collectionBranchId,
        String collectionBranchName,
        Long fulfillmentWarehouseId,
        String fulfillmentWarehouseName,
        OrderStatus status,
        BigDecimal subtotal,
        BigDecimal deliveryFee,
        BigDecimal discount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String paymentMethod,
        String paymentStatus,
        String paymentReference,
        String notes,
        List<OrderItemDto> items,
        List<OrderStatusHistoryDto> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderDto of(Order order, String branchName, String warehouseName,
                              String paymentMethod, String paymentStatus, String paymentReference,
                              List<OrderItemDto> items, List<OrderStatusHistoryDto> history) {
        return new OrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getCustomerPhone(),
                order.getDeliveryType(),
                order.getDeliveryAddress(),
                order.getCollectionBranchId(),
                branchName,
                order.getFulfillmentWarehouseId(),
                warehouseName,
                order.getStatus(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getDiscount(),
                order.getTaxAmount(),
                order.getTotalAmount(),
                paymentMethod != null ? paymentMethod : "CASH",
                paymentStatus != null ? paymentStatus : "PENDING",
                paymentReference,
                order.getNotes(),
                items,
                history,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public static OrderDto of(Order order, String branchName, String warehouseName,
                              List<OrderItemDto> items, List<OrderStatusHistoryDto> history) {
        return of(order, branchName, warehouseName, "CASH", "PENDING", null, items, history);
    }
}
