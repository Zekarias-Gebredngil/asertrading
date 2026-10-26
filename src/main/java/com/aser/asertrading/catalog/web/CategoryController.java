package com.aser.asertrading.catalog.web;

import com.aser.asertrading.catalog.service.CatalogService;
import com.aser.asertrading.catalog.web.dto.CategoryDto;
import com.aser.asertrading.catalog.web.dto.CreateCategoryRequest;
import com.aser.asertrading.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Product Categories", description = "Endpoints for managing product categories and hierarchy")
public class CategoryController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ApiResponse<List<CategoryDto>> getAllCategories() {
        return ApiResponse.success(catalogService.getAllCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ApiResponse<CategoryDto> getCategoryById(@PathVariable Long id) {
        return ApiResponse.success(catalogService.getCategoryById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER', 'WEBSITE_MANAGER')")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody CreateCategoryRequest request, Authentication auth) {
        CategoryDto category = catalogService.createCategory(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Category created successfully", category));
    }
}

