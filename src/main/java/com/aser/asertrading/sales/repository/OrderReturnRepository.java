package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.OrderReturn;
import com.aser.asertrading.sales.domain.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {
    List<OrderReturn> findByOrderId(Long orderId);
    Page<OrderReturn> findByStatus(ReturnStatus status, Pageable pageable);
}

