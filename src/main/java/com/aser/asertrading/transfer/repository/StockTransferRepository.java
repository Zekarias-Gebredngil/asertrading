package com.aser.asertrading.transfer.repository;

import com.aser.asertrading.transfer.domain.StockTransfer;
import com.aser.asertrading.transfer.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, Long> {
    Optional<StockTransfer> findByTransferNumber(String transferNumber);
    boolean existsByTransferNumber(String transferNumber);
    Page<StockTransfer> findByStatus(TransferStatus status, Pageable pageable);
    Page<StockTransfer> findBySourceWarehouseIdOrDestinationWarehouseId(Long sourceId, Long destId, Pageable pageable);

    @Query("SELECT t.status, COUNT(t) FROM StockTransfer t GROUP BY t.status")
    List<Object[]> countTransfersByStatusGroup();

    long countByStatus(TransferStatus status);
}

