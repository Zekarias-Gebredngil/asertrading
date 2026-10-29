package com.aser.asertrading.sales.service;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockBalance;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.sales.domain.*;
import com.aser.asertrading.sales.repository.OrderRepository;
import com.aser.asertrading.sales.repository.OrderReturnRepository;
import com.aser.asertrading.sales.web.dto.ApproveReturnRequest;
import com.aser.asertrading.sales.web.dto.CreateReturnRequest;
import com.aser.asertrading.sales.web.dto.OrderReturnDto;
import com.aser.asertrading.sales.web.dto.OrderReturnItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnService {

    private final OrderReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final AuditService auditService;

    @Transactional
    public OrderReturnDto requestReturn(Long orderId, CreateReturnRequest request, String actor) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Returns can only be requested for DELIVERED orders. Current status: " + order.getStatus());
        }

        OrderReturn orderReturn = OrderReturn.builder()
                .orderId(order.getId())
                .status(ReturnStatus.REQUESTED)
                .reason(request.reason())
                .notes(request.notes())
                .build();

        for (CreateReturnRequest.ReturnItemRequest itemReq : request.items()) {
            if (!productRepository.existsById(itemReq.productId())) {
                throw new ResourceNotFoundException("Product not found with id: " + itemReq.productId());
            }

            OrderReturnItem returnItem = OrderReturnItem.builder()
                    .orderReturn(orderReturn)
                    .productId(itemReq.productId())
                    .quantity(itemReq.quantity())
                    .action(itemReq.action() != null ? itemReq.action() : ReturnAction.RESTOCK)
                    .build();

            orderReturn.getItems().add(returnItem);
        }

        orderReturn = returnRepository.save(orderReturn);
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        auditService.log(actor, "REQUEST_RETURN", "OrderReturn", orderReturn.getId().toString(),
                "Requested return for order " + order.getOrderNumber() + ": " + request.reason(), null);

        return mapToDto(orderReturn);
    }

    @Transactional
    public OrderReturnDto approveReturn(Long returnId, ApproveReturnRequest request, String inspector) {
        OrderReturn orderReturn = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + returnId));

        if (orderReturn.getStatus() != ReturnStatus.REQUESTED) {
            throw new BadRequestException("Return request cannot be approved from status: " + orderReturn.getStatus());
        }

        final Long targetOrderId = orderReturn.getOrderId();
        Order order = orderRepository.findById(targetOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + targetOrderId));

        Long warehouseId = order.getFulfillmentWarehouseId();

        Map<Long, ReturnAction> decisionMap = request.itemDecisions() != null
                ? request.itemDecisions().stream().collect(Collectors.toMap(
                        ApproveReturnRequest.ItemInspectionDecision::returnItemId,
                        ApproveReturnRequest.ItemInspectionDecision::action))
                : Map.of();

        for (OrderReturnItem item : orderReturn.getItems()) {
            ReturnAction action = decisionMap.getOrDefault(item.getId(), item.getAction());
            item.setAction(action);

            // Restock approved items back to inventory atomically
            if (action == ReturnAction.RESTOCK && warehouseId != null) {
                StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseId(item.getProductId(), warehouseId)
                        .orElseGet(() -> StockBalance.builder()
                                .productId(item.getProductId())
                                .warehouseId(warehouseId)
                                .onHandQuantity(BigDecimal.ZERO)
                                .reservedQuantity(BigDecimal.ZERO)
                                .weightedAverageCost(BigDecimal.ZERO)
                                .build());

                balance.setOnHandQuantity(balance.getOnHandQuantity().add(item.getQuantity()));
                stockBalanceRepository.save(balance);

                StockMovement movement = StockMovement.builder()
                        .productId(item.getProductId())
                        .warehouseId(warehouseId)
                        .quantityDelta(item.getQuantity())
                        .movementType(StockMovementType.RETURN_RESTOCK)
                        .referenceType(ReferenceType.SALES_ORDER)
                        .referenceId("RETURN-" + orderReturn.getId() + " (ORD-" + order.getOrderNumber() + ")")
                        .unitCost(balance.getWeightedAverageCost())
                        .totalCost(balance.getWeightedAverageCost().multiply(item.getQuantity()))
                        .actor(inspector)
                        .notes("Customer return restocked after inspection")
                        .build();

                stockMovementRepository.save(movement);
            }
        }

        orderReturn.setStatus(ReturnStatus.RESTOCKED);
        orderReturn.setInspectedBy(inspector);
        orderReturn.setNotes(request.inspectionNotes());
        OrderReturn savedReturn = returnRepository.save(orderReturn);

        order.setStatus(OrderStatus.RETURNED);
        orderRepository.save(order);

        auditService.log(inspector, "APPROVE_RETURN", "OrderReturn", savedReturn.getId().toString(),
                "Approved and inspected return for order " + order.getOrderNumber(), null);

        return mapToDto(savedReturn);
    }

    @Transactional(readOnly = true)
    public Page<OrderReturnDto> getReturns(ReturnStatus status, Pageable pageable) {
        Page<OrderReturn> page = status != null
                ? returnRepository.findByStatus(status, pageable)
                : returnRepository.findAll(pageable);

        return page.map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public OrderReturnDto getReturnById(Long id) {
        OrderReturn orderReturn = returnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Return request not found with id: " + id));
        return mapToDto(orderReturn);
    }

    private OrderReturnDto mapToDto(OrderReturn orderReturn) {
        Order order = orderRepository.findById(orderReturn.getOrderId()).orElse(null);
        String orderNumber = order != null ? order.getOrderNumber() : "UNKNOWN";

        Map<Long, Product> productMap = productRepository.findAllById(
                orderReturn.getItems().stream().map(OrderReturnItem::getProductId).toList()
        ).stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<OrderReturnItemDto> itemDtos = orderReturn.getItems().stream().map(i -> {
            Product p = productMap.get(i.getProductId());
            return OrderReturnItemDto.of(i, p != null ? p.getSku() : "UNKNOWN", p != null ? p.getName() : "UNKNOWN");
        }).toList();

        return OrderReturnDto.of(orderReturn, orderNumber, itemDtos);
    }
}

