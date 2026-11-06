package com.aser.asertrading.organization.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import com.aser.asertrading.organization.web.dto.BranchDto;
import com.aser.asertrading.organization.web.dto.CreateBranchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final WarehouseRepository warehouseRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<BranchDto> getAllBranches() {
        return branchRepository.findAll().stream()
                .map(BranchDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BranchDto> getActiveBranches() {
        return branchRepository.findByIsActiveTrue().stream()
                .map(BranchDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchDto getBranchById(Long id) {
        Branch branch = branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + id));
        return BranchDto.fromEntity(branch);
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

        // Auto-provision a default primary warehouse for the new branch so transfers, orders, and stock work out-of-the-box
        ensureDefaultWarehouse(branch);

        auditService.log(actor, "CREATE_BRANCH", "Branch", branch.getId().toString(),
                "Created branch " + branch.getName() + " (" + branch.getCode() + ")", null);

        return BranchDto.fromEntity(branch);
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

        auditService.log(actor, "UPDATE_BRANCH", "Branch", branch.getId().toString(),
                "Updated branch " + branch.getName(), null);

        return BranchDto.fromEntity(branch);
    }
}

