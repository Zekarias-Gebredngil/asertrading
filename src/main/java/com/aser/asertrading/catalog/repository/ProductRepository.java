package com.aser.asertrading.catalog.repository;

import com.aser.asertrading.catalog.domain.Product;
import com.aser.asertrading.catalog.domain.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySku(String sku);
    Optional<Product> findBySlug(String slug);
    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);

    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isPublished = true AND p.status = 'ACTIVE' " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:quotationOnly IS NULL OR p.isQuotationOnly = :quotationOnly) " +
           "AND (:searchPattern IS NULL OR LOWER(p.name) LIKE :searchPattern OR LOWER(p.sku) LIKE :searchPattern)")
    Page<Product> findPublishedProducts(
            @Param("categoryId") Long categoryId,
            @Param("quotationOnly") Boolean quotationOnly,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "(:status IS NULL OR p.status = :status) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:quotationOnly IS NULL OR p.isQuotationOnly = :quotationOnly) " +
           "AND (:searchPattern IS NULL OR LOWER(p.name) LIKE :searchPattern OR LOWER(p.sku) LIKE :searchPattern)")
    Page<Product> searchProducts(
            @Param("status") ProductStatus status,
            @Param("categoryId") Long categoryId,
            @Param("quotationOnly") Boolean quotationOnly,
            @Param("searchPattern") String searchPattern,
            Pageable pageable);

    List<Product> findByStatusAndIsPublishedTrue(ProductStatus status);
}

