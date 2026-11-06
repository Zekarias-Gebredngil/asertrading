package com.aser.asertrading.organization.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.organization.web.dto.BranchDto;
import com.aser.asertrading.organization.web.dto.BranchWarehouseRequest;
import com.aser.asertrading.organization.web.dto.CreateBranchRequest;
import com.aser.asertrading.organization.web.dto.WarehouseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BranchDto> getAllBranches() {
        Map<Long, List<WarehouseDto>> whMap = warehouseRepository.findAll().stream()
                .map(WarehouseDto::fromEntity)
                .collect(Collectors.groupingBy(WarehouseDto::branchId));

        return branchRepository.findAll().stream()
                .map(b -> BranchDto.fromEntity(b, whMap.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchDto> getActiveBranches() {
        Map<Long, List<WarehouseDto>> whMap = warehouseRepository.findByIsActiveTrue().stream()
                .map(WarehouseDto::fromEntity)
                .collect(Collectors.groupingBy(WarehouseDto::branchId));

        return branchRepository.findByIsActiveTrue().stream()
                .map(b -> BranchDto.fromEntity(b, whMap.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        List<WarehouseDto> warehouses = warehouseRepository.findByBranchId(id).stream()
                .map(WarehouseDto::fromEntity)
                .toList();
        return BranchDto.fromEntity(branch, warehouses);
    }

    @Transactional
    public BranchDto createBranch(CreateBranchRequest request, String actor) {
        String code = request.code().trim().toUpperCase();
        if (branchRepository.existsByCode(code)) {
            throw new BadRequestException("Branch with code '" + code + "' already exists");
        }

        Branch branch = Branch.builder()
                .name(request.name().trim())
                .code(code)
                .address(request.address() != null ? request.address().trim() : null)
                .city(request.city() != null ? request.city().trim() : null)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .email(request.email() != null ? request.email().trim() : null)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        branch = branchRepository.save(branch);

        // Process custom warehouses if provided, otherwise auto-provision default warehouse
        if (request.warehouses() != null && !request.warehouses().isEmpty()) {
            boolean hasDefault = request.warehouses().stream().anyMatch(w -> Boolean.TRUE.equals(w.isDefault()));
            boolean isFirst = true;

            for (BranchWarehouseRequest whReq : request.warehouses()) {
                String whName = (whReq.name() != null && !whReq.name().isBlank())
                        ? whReq.name().trim()
                        : branch.getName() + " Warehouse";
                String whCode = (whReq.code() != null && !whReq.code().isBlank())
                        ? whReq.code().trim().toUpperCase()
                        : code + "-WH";

                if (warehouseRepository.existsByCode(whCode)) {
                    throw new BadRequestException("Warehouse code '" + whCode + "' already exists");
                }

                boolean isDef = Boolean.TRUE.equals(whReq.isDefault()) || (!hasDefault && isFirst);
                isFirst = false;

                Warehouse wh = Warehouse.builder()
                        .branch(branch)
                        .name(whName)
                        .code(whCode)
                        .isDefault(isDef)
                        .isActive(whReq.isActive() != null ? whReq.isActive() : true)
                        .build();
                warehouseRepository.save(wh);
            }
        }

        // Auto-provision a default primary warehouse if none was marked default
        ensureDefaultWarehouse(branch);

        auditService.log(actor, "CREATE_BRANCH", "Branch", branch.getId().toString(),
                "Created branch " + branch.getName() + " (" + branch.getCode() + ")", null);

        List<WarehouseDto> warehouses = warehouseRepository.findByBranchId(branch.getId()).stream()
                .map(WarehouseDto::fromEntity)
                .toList();

        return BranchDto.fromEntity(branch, warehouses);
    }

    public Warehouse ensureDefaultWarehouse(Branch branch) {
        return warehouseRepository.findByBranchIdAndIsDefaultTrue(branch.getId())
                .or(() -> warehouseRepository.findByBranchId(branch.getId()).stream().findFirst())
                .orElseGet(() -> {
                    String baseCode = (branch.getCode() != null && !branch.getCode().isBlank())
                            ? branch.getCode().trim().toUpperCase()
                            : "BR" + branch.getId();
                    String whCode = baseCode + "-WH";
                    if (whCode.length() > 30) {
                        whCode = whCode.substring(0, 30);
                    }
                    int suffix = 1;
                    while (warehouseRepository.existsByCode(whCode)) {
                        whCode = baseCode + "-WH" + suffix++;
                        if (whCode.length() > 30) {
                            whCode = whCode.substring(0, 27) + suffix;
                        }
                    }

                    Warehouse wh = Warehouse.builder()
                            .branch(branch)
                            .name(branch.getName() + " Warehouse")
                            .code(whCode)
                            .isDefault(true)
                            .isActive(true)
                            .build();
                    log.info("[BranchService] Auto-provisioned default warehouse '{}' ({}) for branch '{}'",
                            wh.getName(), wh.getCode(), branch.getName());
                    return warehouseRepository.save(wh);
                });
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAllBranchesHaveWarehouses() {
        List<Branch> branches = branchRepository.findAll();
        for (Branch b : branches) {
            ensureDefaultWarehouse(b);
        }
    }

    @Transactional
    public BranchDto updateBranch(Long id, CreateBranchRequest request, String actor) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));

        String newCode = request.code().trim().toUpperCase();
        if (!branch.getCode().equalsIgnoreCase(newCode) && branchRepository.existsByCode(newCode)) {
            throw new BadRequestException("Branch code '" + newCode + "' is already in use");
        }

        branch.setName(request.name().trim());
        branch.setCode(newCode);
        branch.setAddress(request.address() != null ? request.address().trim() : null);
        branch.setCity(request.city() != null ? request.city().trim() : null);
        branch.setPhone(request.phone() != null ? request.phone().trim() : null);
        branch.setEmail(request.email() != null ? request.email().trim() : null);
        if (request.isActive() != null) {
            branch.setActive(request.isActive());
        }

        branch = branchRepository.save(branch);

        // Process modifications to associated warehouses if provided
        if (request.warehouses() != null && !request.warehouses().isEmpty()) {
            boolean hasDefault = request.warehouses().stream().anyMatch(w -> Boolean.TRUE.equals(w.isDefault()));
            boolean isFirst = true;

            for (BranchWarehouseRequest whReq : request.warehouses()) {
                String whName = (whReq.name() != null && !whReq.name().isBlank())
                        ? whReq.name().trim()
                        : branch.getName() + " Warehouse";

                String whCode = (whReq.code() != null && !whReq.code().isBlank())
                        ? whReq.code().trim().toUpperCase()
                        : newCode + "-WH";

                boolean isDef = Boolean.TRUE.equals(whReq.isDefault()) || (!hasDefault && isFirst);
                isFirst = false;

                if (isDef) {
                    // Clear default flag on other warehouses for this branch
                    warehouseRepository.findByBranchIdAndIsDefaultTrue(branch.getId()).ifPresent(existingDef -> {
                        if (whReq.id() == null || !existingDef.getId().equals(whReq.id())) {
                            existingDef.setDefault(false);
                            warehouseRepository.save(existingDef);
                        }
                    });
                }

                if (whReq.id() != null) {
                    // Update existing warehouse belonging to this branch
                    Warehouse wh = warehouseRepository.findById(whReq.id())
                            .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + whReq.id()));

                    if (!wh.getBranch().getId().equals(branch.getId())) {
                        throw new BadRequestException("Warehouse " + whReq.id() + " does not belong to branch " + branch.getId());
                    }

                    if (!wh.getCode().equalsIgnoreCase(whCode) && warehouseRepository.existsByCode(whCode)) {
                        throw new BadRequestException("Warehouse code '" + whCode + "' is already in use");
                    }

                    wh.setName(whName);
                    wh.setCode(whCode);
                    wh.setDefault(isDef);
                    if (whReq.isActive() != null) {
                        wh.setActive(whReq.isActive());
                    }
                    warehouseRepository.save(wh);
                    log.info("[BranchService] Updated warehouse '{}' ({}) for branch '{}'", wh.getName(), wh.getCode(), branch.getName());
                } else {
                    // Add new warehouse to this branch
                    if (warehouseRepository.existsByCode(whCode)) {
                        throw new BadRequestException("Warehouse code '" + whCode + "' is already in use");
                    }

                    Warehouse newWh = Warehouse.builder()
                            .branch(branch)
                            .name(whName)
                            .code(whCode)
                            .isDefault(isDef)
                            .isActive(whReq.isActive() != null ? whReq.isActive() : true)
                            .build();
                    warehouseRepository.save(newWh);
                    log.info("[BranchService] Added new warehouse '{}' ({}) for branch '{}'", newWh.getName(), newWh.getCode(), branch.getName());
                }
            }
        }

        // Always ensure at least one default warehouse exists for the branch
        ensureDefaultWarehouse(branch);

        auditService.log(actor, "UPDATE_BRANCH", "Branch", branch.getId().toString(),
                "Updated branch " + branch.getName() + " (" + branch.getCode() + ") and associated warehouses", null);

        List<WarehouseDto> warehouses = warehouseRepository.findByBranchId(branch.getId()).stream()
                .map(WarehouseDto::fromEntity)
                .toList();

        return BranchDto.fromEntity(branch, warehouses);
    }
}

