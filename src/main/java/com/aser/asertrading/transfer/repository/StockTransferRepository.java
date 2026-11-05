package com.aser.asertrading.transfer.repository;

import com.aser.asertrading.transfer.domain.StockTransfer;
import com.aser.asertrading.transfer.domain.TransferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT t FROM StockTransfer t WHERE (:status IS NULL OR t.status = :status) AND " +
           "(t.sourceWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId) OR " +
           " t.destinationWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId))")
    Page<StockTransfer> findByBranchAndOptionalStatus(
            @Param("branchId") Long branchId,
            @Param("status") TransferStatus status,
            Pageable pageable);

    @Query("SELECT COUNT(t) FROM StockTransfer t WHERE (:status IS NULL OR t.status = :status) AND " +
           "(t.sourceWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId) OR " +
           " t.destinationWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId))")
    long countByBranchAndOptionalStatus(
            @Param("branchId") Long branchId,
            @Param("status") TransferStatus status);

    @Query("SELECT t.status, COUNT(t) FROM StockTransfer t WHERE " +
           "(t.sourceWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId) OR " +
           " t.destinationWarehouseId IN (SELECT w.id FROM Warehouse w WHERE w.branch.id = :branchId)) " +
           "GROUP BY t.status")
    List<Object[]> countTransfersByStatusGroupAndBranch(@Param("branchId") Long branchId);
}
