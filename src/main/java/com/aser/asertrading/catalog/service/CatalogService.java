package com.aser.asertrading.catalog.service;

import com.aser.asertrading.catalog.domain.ProductCategory;
import com.aser.asertrading.catalog.domain.UnitConversion;
import com.aser.asertrading.catalog.domain.UnitOfMeasure;
import com.aser.asertrading.catalog.repository.ProductCategoryRepository;
import com.aser.asertrading.catalog.repository.UnitConversionRepository;
import com.aser.asertrading.catalog.repository.UnitOfMeasureRepository;
import com.aser.asertrading.catalog.web.dto.*;
import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final UnitOfMeasureRepository unitRepository;
    private final UnitConversionRepository conversionRepository;
    private final ProductCategoryRepository categoryRepository;
    private final AuditService auditService;

    // Unit of Measure
    @Transactional(readOnly = true)
    public List<UnitDto> getAllUnits() {
        return unitRepository.findAll().stream().map(UnitDto::fromEntity).toList();
    }

    @Transactional
    public UnitDto createUnit(CreateUnitRequest request, String actor) {
        String code = request.code().trim().toUpperCase();
        if (unitRepository.existsByCode(code)) {
            throw new BadRequestException("Unit of measure with code '" + code + "' already exists");
        }

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .name(request.name().trim())
                .code(code)
                .symbol(request.symbol() != null ? request.symbol().trim() : code)
                .isBaseUnit(request.isBaseUnit() != null ? request.isBaseUnit() : true)
                .build();

        uom = unitRepository.save(uom);
        auditService.log(actor, "CREATE_UOM", "UnitOfMeasure", uom.getId().toString(), "Created UOM " + uom.getCode(), null);
        return UnitDto.fromEntity(uom);
    }

    // Categories
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByParentCategoryIsNullAndIsActiveTrue().stream().map(CategoryDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        ProductCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryDto.fromEntity(cat);
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request, String actor) {
        String code = request.code().trim().toUpperCase();
        if (categoryRepository.existsByCode(code)) {
            throw new BadRequestException("Category with code '" + code + "' already exists");
        }

        String slug = generateSlug(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
        }

        ProductCategory parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
        }

        ProductCategory cat = ProductCategory.builder()
                .name(request.name().trim())
                .code(code)
                .slug(slug)
                .parentCategory(parent)
                .description(request.description())
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        cat = categoryRepository.save(cat);
        auditService.log(actor, "CREATE_CATEGORY", "ProductCategory", cat.getId().toString(), "Created category " + cat.getName(), null);
        return CategoryDto.fromEntity(cat);
    }

    @Transactional(readOnly = true)
    public BigDecimal getConversionMultiplier(Long fromUnitId, Long toUnitId) {
        if (fromUnitId.equals(toUnitId)) {
            return BigDecimal.ONE;
        }
        Optional<UnitConversion> direct = conversionRepository.findByFromUnitIdAndToUnitId(fromUnitId, toUnitId);
        if (direct.isPresent()) {
            return direct.get().getMultiplier();
        }
        Optional<UnitConversion> inverse = conversionRepository.findByFromUnitIdAndToUnitId(toUnitId, fromUnitId);
        if (inverse.isPresent()) {
            return BigDecimal.ONE.divide(inverse.get().getMultiplier(), 6, java.math.RoundingMode.HALF_UP);
        }
        throw new BadRequestException("No conversion rule found between unit " + fromUnitId + " and unit " + toUnitId);
    }

    private String generateSlug(String input) {
        return input.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .trim();
    }
}

