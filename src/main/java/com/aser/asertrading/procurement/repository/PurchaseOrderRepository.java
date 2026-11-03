package com.aser.asertrading.procurement.repository;

import com.aser.asertrading.procurement.domain.POStatus;
import com.aser.asertrading.procurement.domain.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPoNumber(String poNumber);
    boolean existsByPoNumber(String poNumber);
    Page<PurchaseOrder> findByStatus(POStatus status, Pageable pageable);
    Page<PurchaseOrder> findBySupplierId(Long supplierId, Pageable pageable);
    Page<PurchaseOrder> findByWarehouseId(Long warehouseId, Pageable pageable);
}

