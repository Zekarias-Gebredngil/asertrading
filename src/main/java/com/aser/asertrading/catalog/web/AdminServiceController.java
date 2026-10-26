package com.aser.asertrading.catalog.web;

import com.aser.asertrading.catalog.service.ServiceOfferingService;
import com.aser.asertrading.catalog.web.dto.CreateServiceOfferingRequest;
import com.aser.asertrading.catalog.web.dto.ServiceItemDto;
import com.aser.asertrading.catalog.web.dto.UpdateServiceOfferingRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
@Tag(name = "Corporate Services Administration", description = "Endpoints for managing corporate services and capabilities in the database")
@PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COMPANY_MANAGER', 'WEBSITE_MANAGER')")
public class AdminServiceController {

    private final ServiceOfferingService serviceOfferingService;

    @GetMapping
    @Operation(summary = "List all services with strict pagination for back-office management")
    public ApiResponse<PagedResponse<ServiceItemDto>> getAllServices(
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(PagedResponse.of(serviceOfferingService.getAllServices(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get service details by ID")
    public ApiResponse<ServiceItemDto> getServiceById(@PathVariable Long id) {
        return ApiResponse.success(serviceOfferingService.getServiceById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new corporate service offering in the database")
    public ResponseEntity<ApiResponse<ServiceItemDto>> createService(
            @Valid @RequestBody CreateServiceOfferingRequest request,
            Authentication auth) {

        ServiceItemDto created = serviceOfferingService.createService(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Service created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing corporate service offering")
    public ApiResponse<ServiceItemDto> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceOfferingRequest request,
            Authentication auth) {

        return ApiResponse.success("Service updated successfully", serviceOfferingService.updateService(id, request, auth.getName()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a service offering")
    public ApiResponse<Void> deleteService(@PathVariable Long id, Authentication auth) {
        serviceOfferingService.deleteService(id, auth.getName());
        return ApiResponse.success("Service deactivated successfully", null);
    }
}

