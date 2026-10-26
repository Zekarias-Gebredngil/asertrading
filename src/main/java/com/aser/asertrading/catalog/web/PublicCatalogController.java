package com.aser.asertrading.catalog.web;

import com.aser.asertrading.catalog.service.CatalogService;
import com.aser.asertrading.catalog.service.ProductService;
import com.aser.asertrading.catalog.service.ServiceOfferingService;
import com.aser.asertrading.catalog.web.dto.CategoryDto;
import com.aser.asertrading.catalog.web.dto.PublicProductDto;
import com.aser.asertrading.catalog.web.dto.ServiceItemDto;
import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Website & Catalog", description = "Public-facing endpoints for marketing website and customer storefront")
public class PublicCatalogController {

    private final ProductService productService;
    private final CatalogService catalogService;
    private final ServiceOfferingService serviceOfferingService;

    @GetMapping("/products")
    @Operation(summary = "Search published products (displays public availability, hides internal costs)")
    public ApiResponse<PagedResponse<PublicProductDto>> getPublicProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean quotationOnly,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(productService.getPublicProducts(categoryId, quotationOnly, search, pageable)));
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Get published product details by slug")
    public ApiResponse<PublicProductDto> getPublicProductBySlug(@PathVariable String slug) {
        return ApiResponse.success(productService.getPublicProductBySlug(slug));
    }

    @GetMapping("/categories")
    @Operation(summary = "Get active categories for public navigation")
    public ApiResponse<List<CategoryDto>> getPublicCategories() {
        return ApiResponse.success(catalogService.getActiveCategories());
    }

    @GetMapping("/services")
    @Operation(summary = "List active corporate services directly from database (Printing Press, Branding & Promotion, Garment Materials)")
    public ApiResponse<List<ServiceItemDto>> getPublicServices() {
        return ApiResponse.success(serviceOfferingService.getActiveServices());
    }
}

