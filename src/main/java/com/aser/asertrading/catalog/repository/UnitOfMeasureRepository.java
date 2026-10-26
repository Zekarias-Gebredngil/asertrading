package com.aser.asertrading.catalog.repository;

import com.aser.asertrading.catalog.domain.UnitOfMeasure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {
    Optional<UnitOfMeasure> findByCode(String code);
    boolean existsByCode(String code);
}

