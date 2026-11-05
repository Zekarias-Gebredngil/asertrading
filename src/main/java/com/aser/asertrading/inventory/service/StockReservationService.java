package com.aser.asertrading.inventory.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.InsufficientStockException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.domain.*;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import com.aser.asertrading.inventory.repository.StockMovementRepository;
import com.aser.asertrading.inventory.repository.StockReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockReservationService {

    private final StockBalanceRepository stockBalanceRepository;
    private final StockReservationRepository reservationRepository;
    private final StockMovementRepository movementRepository;
    private final AuditService auditService;

    @Transactional
    public StockReservation reserveStock(Long orderId, Long warehouseId, Long productId, BigDecimal quantity, int expiryHours) {
        StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElseThrow(() -> new InsufficientStockException("No stock balance found for product ID " + productId + " in warehouse " + warehouseId));

        if (balance.getAvailableQuantity().compareTo(quantity) < 0) {
            throw new InsufficientStockException(String.format(
                    "Insufficient stock for product ID %d. Available: %s, Requested: %s",
                    productId, balance.getAvailableQuantity(), quantity
            ));
        }

        // Increase reserved quantity atomically
        balance.setReservedQuantity(balance.getReservedQuantity().add(quantity));
        stockBalanceRepository.save(balance);

        StockReservation reservation = StockReservation.builder()
                .orderId(orderId)
                .warehouseId(warehouseId)
                .productId(productId)
                .reservedQuantity(quantity)
                .status(ReservationStatus.ACTIVE)
                .expiresAt(Instant.now().plus(expiryHours > 0 ? expiryHours : 24, ChronoUnit.HOURS))
                .build();

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void releaseReservationForOrder(Long orderId, String actor) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (StockReservation res : reservations) {
            stockBalanceRepository.findByProductIdAndWarehouseId(res.getProductId(), res.getWarehouseId())
                    .ifPresent(balance -> {
                        BigDecimal newReserved = balance.getReservedQuantity().subtract(res.getReservedQuantity());
                        balance.setReservedQuantity(newReserved.compareTo(BigDecimal.ZERO) > 0 ? newReserved : BigDecimal.ZERO);
                        stockBalanceRepository.save(balance);
                    });

            res.setStatus(ReservationStatus.RELEASED);
            reservationRepository.save(res);
        }

        if (!reservations.isEmpty()) {
            auditService.log(actor, "RELEASE_STOCK_RESERVATION", "Order", orderId.toString(),
                    "Released stock reservations for cancelled/expired order", null);
        }
    }

    @Transactional
    public void fulfillReservationForOrder(Long orderId, String actor) {
        List<StockReservation> reservations = reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE);
        for (StockReservation res : reservations) {
            StockBalance balance = stockBalanceRepository.findByProductIdAndWarehouseIdForUpdate(res.getProductId(), res.getWarehouseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Stock balance not found for product " + res.getProductId()));

            // Decrement reserved and on-hand
            BigDecimal newReserved = balance.getReservedQuantity().subtract(res.getReservedQuantity());
            balance.setReservedQuantity(newReserved.compareTo(BigDecimal.ZERO) > 0 ? newReserved : BigDecimal.ZERO);

            BigDecimal newOnHand = balance.getOnHandQuantity().subtract(res.getReservedQuantity());
            balance.setOnHandQuantity(newOnHand.compareTo(BigDecimal.ZERO) > 0 ? newOnHand : BigDecimal.ZERO);
            stockBalanceRepository.save(balance);

            res.setStatus(ReservationStatus.FULFILLED);
            reservationRepository.save(res);

            // Record stock issue movement at current weighted-average cost
            BigDecimal unitCost = balance.getWeightedAverageCost();
            BigDecimal totalCost = unitCost != null ? unitCost.multiply(res.getReservedQuantity()) : BigDecimal.ZERO;

            StockMovement movement = StockMovement.builder()
                    .productId(res.getProductId())
                    .warehouseId(res.getWarehouseId())
                    .quantityDelta(res.getReservedQuantity().negate())
                    .movementType(StockMovementType.ISSUE)
                    .referenceType(ReferenceType.SALES_ORDER)
                    .referenceId(orderId.toString())
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .actor(actor)
                    .notes("Order fulfillment: order #" + orderId)
                    .build();

            movementRepository.save(movement);
        }
    }
}

