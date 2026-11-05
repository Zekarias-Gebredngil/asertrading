package com.aser.asertrading.inventory.repository;

import com.aser.asertrading.inventory.domain.ReferenceType;
import com.aser.asertrading.inventory.domain.StockMovement;
import com.aser.asertrading.inventory.domain.StockMovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    Page<StockMovement> findByProductId(Long productId, Pageable pageable);
    Page<StockMovement> findByWarehouseId(Long warehouseId, Pageable pageable);
    Page<StockMovement> findByMovementType(StockMovementType movementType, Pageable pageable);
    List<StockMovement> findByReferenceTypeAndReferenceId(ReferenceType referenceType, String referenceId);

    @Query("SELECT m FROM StockMovement m WHERE m.warehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId)")
    Page<StockMovement> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT m FROM StockMovement m WHERE m.warehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId) " +
           "AND (:productId IS NULL OR m.productId = :productId) " +
           "AND (:warehouseId IS NULL OR m.warehouseId = :warehouseId) " +
           "AND (:movementType IS NULL OR m.movementType = :movementType)")
    Page<StockMovement> findWithFiltersAndBranch(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId,
            @Param("movementType") StockMovementType movementType,
            @Param("branchId") Long branchId,
            Pageable pageable);
}
