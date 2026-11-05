package com.aser.asertrading.inventory.repository;

import com.aser.asertrading.inventory.domain.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBalanceRepository extends JpaRepository<StockBalance, Long> {

    Optional<StockBalance> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBalance b WHERE b.productId = :productId AND b.warehouseId = :warehouseId")
    Optional<StockBalance> findByProductIdAndWarehouseIdForUpdate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    List<StockBalance> findByProductId(Long productId);

    List<StockBalance> findByWarehouseId(Long warehouseId);

    Page<StockBalance> findByWarehouseId(Long warehouseId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(b.onHandQuantity), 0) FROM StockBalance b WHERE b.productId = :productId")
    BigDecimal getTotalOnHandByProduct(@Param("productId") Long productId);

    @Query("SELECT COALESCE(SUM(b.reservedQuantity), 0) FROM StockBalance b WHERE b.productId = :productId")
    BigDecimal getTotalReservedByProduct(@Param("productId") Long productId);

    @Query("SELECT b FROM StockBalance b WHERE (b.onHandQuantity - b.reservedQuantity) <= :threshold")
    List<StockBalance> findLowStockBalances(@Param("threshold") BigDecimal threshold);

    @Query("SELECT COUNT(DISTINCT b.productId) FROM StockBalance b")
    long countDistinctProducts();

    @Query("SELECT COALESCE(SUM(b.onHandQuantity), 0) FROM StockBalance b")
    BigDecimal getTotalOnHand();

    @Query("SELECT COALESCE(SUM(b.reservedQuantity), 0) FROM StockBalance b")
    BigDecimal getTotalReserved();

    @Query("SELECT COALESCE(SUM(b.onHandQuantity - b.reservedQuantity), 0) FROM StockBalance b")
    BigDecimal getTotalAvailable();
}

