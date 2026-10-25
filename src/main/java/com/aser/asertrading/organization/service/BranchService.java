package com.aser.asertrading.organization.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.organization.domain.Branch;
import com.aser.asertrading.organization.repository.BranchRepository;
import com.aser.asertrading.organization.web.dto.BranchDto;
import com.aser.asertrading.organization.web.dto.CreateBranchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
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

        auditService.log(actor, "CREATE_BRANCH", "Branch", branch.getId().toString(),
                "Created branch " + branch.getName() + " (" + branch.getCode() + ")", null);

        return BranchDto.fromEntity(branch);
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

