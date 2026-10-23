package com.aser.asertrading.identity.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.ResourceNotFoundException;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;
import com.aser.asertrading.identity.domain.UserBranchAccess;
import com.aser.asertrading.identity.repository.UserBranchAccessRepository;
import com.aser.asertrading.identity.repository.UserRepository;
import com.aser.asertrading.identity.web.dto.CreateUserRequest;
import com.aser.asertrading.identity.web.dto.UpdateUserRequest;
import com.aser.asertrading.identity.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserBranchAccessRepository userBranchAccessRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<UserDto> getUsers(Role role, Long branchId, Pageable pageable) {
        if (role != null) {
            return userRepository.findByRole(role, pageable).map(UserDto::fromEntity);
        }
        if (branchId != null) {
            return userRepository.findByAssignedBranchId(branchId, pageable).map(UserDto::fromEntity);
        }
        return userRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request, String actor) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username '" + request.username() + "' already exists");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email '" + request.email() + "' already exists");
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .fullName(request.fullName().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .role(request.role())
                .assignedBranchId(request.assignedBranchId())
                .passwordHash(passwordEncoder.encode(request.password()))
                .isActive(true)
                .build();

        user = userRepository.save(user);

        if (user.getAssignedBranchId() != null) {
            userBranchAccessRepository.save(UserBranchAccess.builder()
                    .userId(user.getId())
                    .branchId(user.getAssignedBranchId())
                    .build());
        }

        auditService.log(actor, "CREATE_USER", "User", user.getId().toString(),
                "Created user " + user.getUsername() + " with role " + user.getRole(), null);

        return UserDto.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long id, String username) {
        if (id == null || username == null) return false;
        return userRepository.findById(id)
                .map(u -> u.getUsername().equalsIgnoreCase(username))
                .orElse(false);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request, String actor) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPhone(request.phone() != null ? request.phone().trim() : null);

        boolean isAdmin = userRepository.findByUsername(actor)
                .map(u -> u.getRole() == Role.SYSTEM_ADMINISTRATOR)
                .orElse(false);

        if (isAdmin) {
            if (request.role() != null) {
                user.setRole(request.role());
            }
            if (request.assignedBranchId() != null) {
                user.setAssignedBranchId(request.assignedBranchId());
            }
            if (request.isActive() != null) {
                user.setActive(request.isActive());
            }
        }

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        user = userRepository.save(user);

        auditService.log(actor, "UPDATE_USER", "User", user.getId().toString(),
                "Updated user " + user.getUsername(), null);

        return UserDto.fromEntity(user);
    }

    @Transactional
    public void grantBranchAccess(Long userId, Long branchId, String actor) {
        if (!userBranchAccessRepository.existsByUserIdAndBranchId(userId, branchId)) {
            userBranchAccessRepository.save(UserBranchAccess.builder()
                    .userId(userId)
                    .branchId(branchId)
                    .build());

            auditService.log(actor, "GRANT_BRANCH_ACCESS", "UserBranchAccess",
                    userId + "-" + branchId, "Granted user " + userId + " access to branch " + branchId, null);
        }
    }

    @Transactional
    public void revokeBranchAccess(Long userId, Long branchId, String actor) {
        userBranchAccessRepository.deleteByUserIdAndBranchId(userId, branchId);
        auditService.log(actor, "REVOKE_BRANCH_ACCESS", "UserBranchAccess",
                userId + "-" + branchId, "Revoked user " + userId + " access to branch " + branchId, null);
    }

    @Transactional(readOnly = true)
    public List<Long> getAccessibleBranchIds(Long userId) {
        return userBranchAccessRepository.findByUserId(userId).stream()
                .map(UserBranchAccess::getBranchId)
                .toList();
    }
}

