package com.aser.asertrading.catalog.repository;

import com.aser.asertrading.catalog.domain.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {
    Optional<UnitConversion> findByFromUnitIdAndToUnitId(Long fromUnitId, Long toUnitId);
}

