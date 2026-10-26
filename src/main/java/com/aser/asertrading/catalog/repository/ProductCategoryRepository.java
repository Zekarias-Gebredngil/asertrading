package com.aser.asertrading.catalog.repository;

import com.aser.asertrading.catalog.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findBySlug(String slug);
    Optional<ProductCategory> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsBySlug(String slug);
    List<ProductCategory> findByParentCategoryIsNullAndIsActiveTrue();
    List<ProductCategory> findByIsActiveTrue();
}

