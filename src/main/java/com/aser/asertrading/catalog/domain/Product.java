package com.aser.asertrading.catalog.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_products_sku", columnList = "sku", unique = true),
        @Index(name = "idx_products_slug", columnList = "slug", unique = true),
        @Index(name = "idx_products_barcode", columnList = "barcode"),
        @Index(name = "idx_products_category", columnList = "category_id"),
        @Index(name = "idx_products_status", columnList = "status"),
        @Index(name = "idx_products_published", columnList = "is_published")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String sku;

    @Column(length = 60)
    private String barcode;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_unit_id", nullable = false)
    private UnitOfMeasure baseUnit;

    @Column(name = "purchase_cost", nullable = false, precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal purchaseCost = BigDecimal.ZERO;

    @Column(name = "selling_price", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal sellingPrice = BigDecimal.ZERO;

    @Column(name = "reorder_threshold", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal reorderThreshold = BigDecimal.valueOf(10);

    @Column(name = "preferred_supplier_id")
    private Long preferredSupplierId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(name = "is_quotation_only", nullable = false)
    @Builder.Default
    private boolean isQuotationOnly = false;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean isPublished = true;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "specifications", columnDefinition = "TEXT")
    private String specifications;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }
}

