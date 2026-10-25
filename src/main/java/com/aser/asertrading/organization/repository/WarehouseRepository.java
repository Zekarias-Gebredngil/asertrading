package com.aser.asertrading.organization.repository;

import com.aser.asertrading.organization.domain.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    Optional<Warehouse> findByCode(String code);
    boolean existsByCode(String code);
    List<Warehouse> findByBranchId(Long branchId);
    List<Warehouse> findByBranchIdAndIsActiveTrue(Long branchId);
    List<Warehouse> findByIsActiveTrue();
    Optional<Warehouse> findByBranchIdAndIsDefaultTrue(Long branchId);
}

