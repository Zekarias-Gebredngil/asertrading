package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByCustomerEmail(String email, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByCollectionBranchId(Long branchId, Pageable pageable);
}

