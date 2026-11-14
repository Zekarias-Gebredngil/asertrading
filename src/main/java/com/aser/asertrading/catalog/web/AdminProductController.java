package com.aser.asertrading.catalog.web;

import com.aser.asertrading.catalog.domain.ProductStatus;
import com.aser.asertrading.catalog.service.ProductService;
import com.aser.asertrading.catalog.web.dto.CreateProductRequest;
import com.aser.asertrading.catalog.web.dto.ProductDto;
import com.aser.asertrading.catalog.web.dto.UpdateProductRequest;
import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Administration", description = "Endpoints for staff inventory and catalog management")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search and list products with internal costs and full inventory details")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER', 'SALES_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<PagedResponse<ProductDto>> searchProducts(
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean quotationOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(
                PagedResponse.of(productService.searchProducts(status, categoryId, quotationOnly, search, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER', 'SALES_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<ProductDto> getProductById(@PathVariable Long id) {
        return ApiResponse.success(productService.getProductById(id));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product details by SKU")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'INVENTORY_MANAGER', 'BRANCH_MANAGER', 'STOREKEEPER', 'PROCUREMENT_OFFICER', 'SALES_OFFICER', 'ACCOUNTANT_AUDITOR')")
    public ApiResponse<ProductDto> getProductBySku(@PathVariable String sku) {
        return ApiResponse.success(productService.getProductBySku(sku));
    }

    @PostMapping
    @Operation(summary = "Create a new product in the catalog")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody CreateProductRequest request,
            Authentication auth) {
        ProductDto product = productService.createProduct(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ApiResponse<ProductDto> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            Authentication auth) {

        return ApiResponse.success("Product updated successfully",
                productService.updateProduct(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive a product (soft delete)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ApiResponse<Void> archiveProduct(@PathVariable Long id, Authentication auth) {
        productService.archiveProduct(id, auth.getName());
        return ApiResponse.success("Product archived successfully", null);
    }
}
