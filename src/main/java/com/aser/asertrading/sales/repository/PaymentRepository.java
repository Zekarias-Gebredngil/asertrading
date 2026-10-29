package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.Payment;
import com.aser.asertrading.sales.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
    Optional<Payment> findByReferenceNumber(String referenceNumber);
}

