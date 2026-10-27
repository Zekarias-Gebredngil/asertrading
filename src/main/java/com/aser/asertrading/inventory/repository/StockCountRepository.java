package com.aser.asertrading.inventory.repository;

import com.aser.asertrading.inventory.domain.StockCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockCountRepository extends JpaRepository<StockCount, Long> {
    Page<StockCount> findByWarehouseId(Long warehouseId, Pageable pageable);
}

