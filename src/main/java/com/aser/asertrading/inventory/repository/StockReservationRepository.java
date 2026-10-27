package com.aser.asertrading.inventory.repository;

import com.aser.asertrading.inventory.domain.ReservationStatus;
import com.aser.asertrading.inventory.domain.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByOrderIdAndStatus(Long orderId, ReservationStatus status);
    List<StockReservation> findByOrderId(Long orderId);
    List<StockReservation> findByStatusAndExpiresAtBefore(ReservationStatus status, Instant now);
}

