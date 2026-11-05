package com.aser.asertrading.sales.repository;

import com.aser.asertrading.sales.domain.DeliveryType;
import com.aser.asertrading.sales.domain.Order;
import com.aser.asertrading.sales.domain.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    boolean existsByOrderNumber(String orderNumber);
    Page<Order> findByCustomerId(Long customerId, Pageable pageable);
    Page<Order> findByCustomerEmail(String email, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByCollectionBranchId(Long branchId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != :excludedStatus")
    BigDecimal sumTotalAmountExcludingStatus(@Param("excludedStatus") OrderStatus excludedStatus);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status != :excludedStatus")
    long countExcludingStatus(@Param("excludedStatus") OrderStatus excludedStatus);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatusGroup();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.deliveryType = :deliveryType")
    long countByDeliveryType(@Param("deliveryType") DeliveryType deliveryType);
}

