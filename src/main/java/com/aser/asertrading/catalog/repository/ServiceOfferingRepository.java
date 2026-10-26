package com.aser.asertrading.catalog.repository;

import com.aser.asertrading.catalog.domain.ServiceOffering;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceOfferingRepository extends JpaRepository<ServiceOffering, Long> {

    List<ServiceOffering> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<ServiceOffering> findAllByOrderByDisplayOrderAsc();

    Page<ServiceOffering> findAllByOrderByDisplayOrderAsc(Pageable pageable);

    Optional<ServiceOffering> findByCode(String code);

    boolean existsByCode(String code);
}

