package com.aser.asertrading.procurement.repository;

import com.aser.asertrading.procurement.domain.GoodsReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    Optional<GoodsReceipt> findByGrnNumber(String grnNumber);
    List<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);
    Page<GoodsReceipt> findByWarehouseId(Long warehouseId, Pageable pageable);
}

