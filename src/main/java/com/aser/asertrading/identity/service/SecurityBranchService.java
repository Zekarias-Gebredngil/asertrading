package com.aser.asertrading.identity.service;

import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.repository.UserRepository;
import com.aser.asertrading.organization.domain.Warehouse;
import com.aser.asertrading.organization.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityBranchService {

    private final UserRepository userRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Retrieves the current authenticated user's principal.
     */
    public Optional<UserPrincipal> getCurrentUserPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof UserPrincipal up) {
            return Optional.of(up);
        }
        if (auth.getName() != null && !auth.getName().isBlank() && !"anonymousUser".equals(auth.getName())) {
            return userRepository.findByUsername(auth.getName()).map(UserPrincipal::new);
        }
        return Optional.empty();
    }

    /**
     * Determines whether the current authenticated user has multi-branch / global access.
     * Roles with global visibility: SYSTEM_ADMINISTRATOR, COMPANY_MANAGER, INVENTORY_MANAGER, ACCOUNTANT_AUDITOR.
     * When there is no SecurityContext (internal service or integration test invocation), returns true.
     */
    public boolean hasGlobalBranchAccess() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return true;
        }
        return getCurrentUserPrincipal()
                .map(up -> up.getRole() == Role.SYSTEM_ADMINISTRATOR
                        || up.getRole() == Role.COMPANY_MANAGER
                        || up.getRole() == Role.INVENTORY_MANAGER
                        || up.getRole() == Role.ACCOUNTANT_AUDITOR)
                .orElse(false);
    }

    /**
     * Returns the caller's assigned branch ID, or null if unassigned / global.
     */
    public Long getCallerBranchId() {
        return getCurrentUserPrincipal()
                .map(UserPrincipal::getAssignedBranchId)
                .orElse(null);
    }

    /**
     * Resolves the effective branch ID to query.
     * If the user has global access or no active authentication, honors the requested branchId.
     * If the user is scoped to a dedicated branch, overrides with the caller's assigned branch ID.
     */
    public Long resolveEffectiveBranchId(Long requestedBranchId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || hasGlobalBranchAccess()) {
            return requestedBranchId;
        }
        Long assigned = getCallerBranchId();
        if (assigned == null) {
            log.warn("[SecurityBranchService] User has branch-restricted role but no assignedBranchId; defaulting to 1");
            return 1L;
        }
        return assigned;
    }

    /**
     * Asserts that the caller is permitted to access resources belonging to targetBranchId.
     */
    public void validateBranchAccess(Long targetBranchId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || hasGlobalBranchAccess()) {
            return;
        }
        Long callerBranchId = getCallerBranchId();
        if (callerBranchId == null || !callerBranchId.equals(targetBranchId)) {
            throw new AccessDeniedException("Access denied: You do not have permission to view or manage resources for branch ID: " + targetBranchId);
        }
    }

    /**
     * Asserts that the caller is permitted to access a specific warehouse.
     */
    public void validateWarehouseAccess(Long warehouseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || hasGlobalBranchAccess() || warehouseId == null) {
            return;
        }
        Long callerBranchId = getCallerBranchId();
        Warehouse wh = warehouseRepository.findById(warehouseId).orElse(null);
        if (wh != null && wh.getBranch() != null && !wh.getBranch().getId().equals(callerBranchId)) {
            throw new AccessDeniedException("Access denied: Warehouse " + warehouseId + " belongs to branch " + wh.getBranch().getId()
                    + ", but your assigned branch is " + callerBranchId);
        }
    }

    /**
     * Checks whether a stock transfer involves the caller's assigned branch as either source or destination.
     */
    public boolean canAccessTransfer(Long sourceWarehouseId, Long destinationWarehouseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || hasGlobalBranchAccess()) {
            return true;
        }
        Long callerBranchId = getCallerBranchId();
        if (callerBranchId == null) {
            return false;
        }
        Warehouse src = warehouseRepository.findById(sourceWarehouseId).orElse(null);
        Warehouse dst = warehouseRepository.findById(destinationWarehouseId).orElse(null);

        boolean isSource = src != null && src.getBranch() != null && callerBranchId.equals(src.getBranch().getId());
        boolean isDest = dst != null && dst.getBranch() != null && callerBranchId.equals(dst.getBranch().getId());
        return isSource || isDest;
    }

    /**
     * Checks whether an online order belongs to the caller's assigned branch.
     * Evaluates true if user has global branch access, or if the order's collection branch
     * or fulfillment warehouse belongs to the caller's assigned branch.
     */
    public boolean canAccessOrder(Long collectionBranchId, Long fulfillmentWarehouseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || hasGlobalBranchAccess()) {
            return true;
        }
        Long callerBranchId = getCallerBranchId();
        if (callerBranchId == null) {
            return false;
        }
        if (collectionBranchId != null && collectionBranchId.equals(callerBranchId)) {
            return true;
        }
        if (fulfillmentWarehouseId != null) {
            Warehouse wh = warehouseRepository.findById(fulfillmentWarehouseId).orElse(null);
            if (wh != null && wh.getBranch() != null && callerBranchId.equals(wh.getBranch().getId())) {
                return true;
            }
        }
        return false;
    }
}
