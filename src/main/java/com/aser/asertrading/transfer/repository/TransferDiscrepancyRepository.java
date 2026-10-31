package com.aser.asertrading.transfer.repository;

import com.aser.asertrading.transfer.domain.TransferDiscrepancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferDiscrepancyRepository extends JpaRepository<TransferDiscrepancy, Long> {
    List<TransferDiscrepancy> findByTransferId(Long transferId);
}

