package com.aser.asertrading.organization.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.organization.web.dto.CreateWarehouseRequest;
import com.aser.asertrading.organization.web.dto.WarehouseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final BranchRepository branchRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<WarehouseDto> getAllWarehouses() {
        return warehouseRepository.findAll().stream()
                .map(WarehouseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WarehouseDto> getWarehousesByBranch(Long branchId) {
        return warehouseRepository.findByBranchId(branchId).stream()
                .map(WarehouseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WarehouseDto getWarehouseById(Long id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
        return WarehouseDto.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest request, String actor) {
        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));

        String code = request.code().trim().toUpperCase();
        if (warehouseRepository.existsByCode(code)) {
            throw new BadRequestException("Warehouse code '" + code + "' already exists");
        }

        boolean isDefault = request.isDefault() != null && request.isDefault();
        if (isDefault) {
            warehouseRepository.findByBranchIdAndIsDefaultTrue(branch.getId()).ifPresent(wh -> {
                wh.setDefault(false);
                warehouseRepository.save(wh);
            });
        }

        Warehouse warehouse = Warehouse.builder()
                .branch(branch)
                .name(request.name().trim())
                .code(code)
                .isDefault(isDefault)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        warehouse = warehouseRepository.save(warehouse);

        auditService.log(actor, "CREATE_WAREHOUSE", "Warehouse", warehouse.getId().toString(),
                "Created warehouse " + warehouse.getName() + " (" + warehouse.getCode() + ") for branch " + branch.getName(), null);

        return WarehouseDto.fromEntity(warehouse);
    }

    @Transactional
    public WarehouseDto updateWarehouse(Long id, CreateWarehouseRequest request, String actor) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));

        Branch branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.branchId()));

        String newCode = request.code().trim().toUpperCase();
        if (!warehouse.getCode().equalsIgnoreCase(newCode) && warehouseRepository.existsByCode(newCode)) {
            throw new BadRequestException("Warehouse code '" + newCode + "' is already in use");
        }

        boolean isDefault = request.isDefault() != null && request.isDefault();
        if (isDefault && !warehouse.isDefault()) {
            warehouseRepository.findByBranchIdAndIsDefaultTrue(branch.getId()).ifPresent(wh -> {
                wh.setDefault(false);
                warehouseRepository.save(wh);
            });
        }

        warehouse.setBranch(branch);
        warehouse.setName(request.name().trim());
        warehouse.setCode(newCode);
        warehouse.setDefault(isDefault);
        if (request.isActive() != null) {
            warehouse.setActive(request.isActive());
        }

        warehouse = warehouseRepository.save(warehouse);

        auditService.log(actor, "UPDATE_WAREHOUSE", "Warehouse", warehouse.getId().toString(),
                "Updated warehouse " + warehouse.getName(), null);

        return WarehouseDto.fromEntity(warehouse);
    }
}

