package com.aser.asertrading.inventory.repository;

import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);
    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);
    Page<StockMovement> findByMovementType(StockMovementType movementType, Pageable pageable);
    List<StockMovement> findByReferenceTypeAndReferenceId(ReferenceType referenceType, String referenceId);
}

