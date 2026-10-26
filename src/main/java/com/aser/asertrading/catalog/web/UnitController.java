package com.aser.asertrading.catalog.web;

import com.aser.asertrading.catalog.service.CatalogService;
import com.aser.asertrading.catalog.web.dto.CreateUnitRequest;
import com.aser.asertrading.catalog.web.dto.UnitDto;
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
@RequestMapping("/api/v1/units")
@RequiredArgsConstructor
@Tag(name = "Units of Measure", description = "Endpoints for managing inventory units (Piece, Meter, Roll, Kilogram, etc.)")
public class UnitController {

    private final CatalogService catalogService;

    @GetMapping
    @Operation(summary = "Get all configured units of measure")
    public ApiResponse<List<UnitDto>> getAllUnits() {
        return ApiResponse.success(catalogService.getAllUnits());
    }

    @PostMapping
    @Operation(summary = "Create a new unit of measure")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'INVENTORY_MANAGER')")
    public ResponseEntity<ApiResponse<UnitDto>> createUnit(@Valid @RequestBody CreateUnitRequest request, Authentication auth) {
        UnitDto uom = catalogService.createUnit(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Unit created successfully", uom));
    }
}

