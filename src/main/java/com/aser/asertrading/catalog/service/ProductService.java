package com.aser.asertrading.catalog.service;

import com.aser.asertrading.catalog.domain.*;
import com.aser.asertrading.catalog.repository.ProductCategoryRepository;
import com.aser.asertrading.catalog.repository.ProductRepository;
import com.aser.asertrading.catalog.repository.UnitOfMeasureRepository;
import com.aser.asertrading.catalog.web.dto.CreateProductRequest;
import com.aser.asertrading.catalog.web.dto.ProductDto;
import com.aser.asertrading.catalog.web.dto.PublicProductDto;
import com.aser.asertrading.catalog.web.dto.UpdateProductRequest;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.inventory.repository.StockBalanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final UnitOfMeasureRepository unitRepository;
    private final StockBalanceRepository stockBalanceRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<ProductDto> searchProducts(ProductStatus status, Long categoryId, Boolean quotationOnly, String search, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(status, categoryId, quotationOnly, toSearchPattern(search), pageable);
        return products.map(this::mapToProductDto);
    }

    private String toSearchPattern(String search) {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }
        return "%" + search.trim().toLowerCase() + "%";
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToProductDto(product);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return mapToProductDto(product);
    }

    // Public catalog methods (strictly without supplier costs)
    @Transactional(readOnly = true)
    public Page<PublicProductDto> getPublicProducts(Long categoryId, Boolean quotationOnly, String search, Pageable pageable) {
        Page<Product> products = productRepository.findPublishedProducts(categoryId, quotationOnly, toSearchPattern(search), pageable);
        return products.map(p -> PublicProductDto.fromEntity(p, determineAvailability(p)));
    }

    @Transactional(readOnly = true)
    public PublicProductDto getPublicProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .filter(p -> p.isPublished() && p.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Published product not found with slug: " + slug));

        return PublicProductDto.fromEntity(product, determineAvailability(product));
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request, String actor) {
        String sku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(sku)) {
            throw new BadRequestException("Product with SKU '" + sku + "' already exists");
        }

        String slug = generateSlug(request.name());
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + sku.toLowerCase();
        }

        ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        UnitOfMeasure baseUnit = unitRepository.findById(request.baseUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Base unit not found with id: " + request.baseUnitId()));

        Product product = Product.builder()
                .sku(sku)
                .barcode(request.barcode() != null ? request.barcode().trim() : null)
                .name(request.name().trim())
                .slug(slug)
                .description(request.description())
                .category(category)
                .baseUnit(baseUnit)
                .purchaseCost(request.purchaseCost() != null ? request.purchaseCost() : BigDecimal.ZERO)
                .sellingPrice(request.sellingPrice() != null ? request.sellingPrice() : BigDecimal.ZERO)
                .reorderThreshold(request.reorderThreshold() != null ? request.reorderThreshold() : BigDecimal.valueOf(10))
                .preferredSupplierId(request.preferredSupplierId())
                .status(request.status() != null ? request.status() : ProductStatus.ACTIVE)
                .isQuotationOnly(request.isQuotationOnly() != null ? request.isQuotationOnly() : false)
                .isPublished(request.isPublished() != null ? request.isPublished() : true)
                .imageUrl(request.imageUrl())
                .specifications(request.specifications())
                .build();

        product = productRepository.save(product);

        auditService.log(actor, "CREATE_PRODUCT", "Product", product.getId().toString(),
                "Created product SKU: " + product.getSku() + ", Name: " + product.getName(), null);

        return mapToProductDto(product);
    }

    @Transactional
    public ProductDto updateProduct(Long id, UpdateProductRequest request, String actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        ProductCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        UnitOfMeasure baseUnit = unitRepository.findById(request.baseUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("Base unit not found with id: " + request.baseUnitId()));

        BigDecimal oldCost = product.getPurchaseCost();
        BigDecimal oldPrice = product.getSellingPrice();

        product.setName(request.name().trim());
        product.setBarcode(request.barcode() != null ? request.barcode().trim() : null);
        product.setDescription(request.description());
        product.setCategory(category);
        product.setBaseUnit(baseUnit);
        if (request.purchaseCost() != null) {
            product.setPurchaseCost(request.purchaseCost());
        }
        if (request.sellingPrice() != null) {
            product.setSellingPrice(request.sellingPrice());
        }
        if (request.reorderThreshold() != null) {
            product.setReorderThreshold(request.reorderThreshold());
        }
        product.setPreferredSupplierId(request.preferredSupplierId());
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.isQuotationOnly() != null) {
            product.setQuotationOnly(request.isQuotationOnly());
        }
        if (request.isPublished() != null) {
            product.setPublished(request.isPublished());
        }
        product.setImageUrl(request.imageUrl());
        product.setSpecifications(request.specifications());

        product = productRepository.save(product);

        String details = String.format("Updated product SKU %s. Cost: %s -> %s, Price: %s -> %s",
                product.getSku(), oldCost, product.getPurchaseCost(), oldPrice, product.getSellingPrice());
        auditService.log(actor, "UPDATE_PRODUCT", "Product", product.getId().toString(), details, null);

        return mapToProductDto(product);
    }

    @Transactional
    public void archiveProduct(Long id, String actor) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setStatus(ProductStatus.ARCHIVED);
        product.setPublished(false);
        productRepository.save(product);

        auditService.log(actor, "ARCHIVE_PRODUCT", "Product", product.getId().toString(),
                "Archived product SKU: " + product.getSku(), null);
    }

    private ProductDto mapToProductDto(Product product) {
        BigDecimal onHand = stockBalanceRepository.getTotalOnHandByProduct(product.getId());
        BigDecimal reserved = stockBalanceRepository.getTotalReservedByProduct(product.getId());
        BigDecimal available = onHand != null && reserved != null ? onHand.subtract(reserved) : BigDecimal.ZERO;
        if (available.compareTo(BigDecimal.ZERO) < 0) {
            available = BigDecimal.ZERO;
        }

        ProductAvailability availability = determineAvailability(product, available);
        return ProductDto.fromEntity(product, onHand, available, availability);
    }

    public ProductAvailability determineAvailability(Product product) {
        BigDecimal onHand = stockBalanceRepository.getTotalOnHandByProduct(product.getId());
        BigDecimal reserved = stockBalanceRepository.getTotalReservedByProduct(product.getId());
        BigDecimal available = onHand != null && reserved != null ? onHand.subtract(reserved) : BigDecimal.ZERO;
        return determineAvailability(product, available);
    }

    private ProductAvailability determineAvailability(Product product, BigDecimal available) {
        if (product.isQuotationOnly()) {
            return ProductAvailability.AVAILABLE_ON_REQUEST;
        }
        if (available == null || available.compareTo(BigDecimal.ZERO) <= 0) {
            return ProductAvailability.OUT_OF_STOCK;
        }
        if (available.compareTo(product.getReorderThreshold()) <= 0) {
            return ProductAvailability.LIMITED_AVAILABILITY;
        }
        return ProductAvailability.IN_STOCK;
    }

    private String generateSlug(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}

