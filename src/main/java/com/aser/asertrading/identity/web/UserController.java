package com.aser.asertrading.identity.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.common.dto.PagedResponse;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.service.UserService;
import com.aser.asertrading.identity.web.dto.CreateUserRequest;
import com.aser.asertrading.identity.web.dto.UpdateUserRequest;
import com.aser.asertrading.identity.web.dto.UserDto;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Administration", description = "Endpoints for managing staff and customer user accounts")
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "List users with optional role or branch filter")
    public ApiResponse<PagedResponse<UserDto>> getUsers(
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Long branchId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PagedResponse.of(userService.getUsers(role, branchId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or @userService.isOwner(#id, authentication.name)")
    @Operation(summary = "Get user details by ID")
    public ApiResponse<UserDto> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Create a new staff or administrative user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        UserDto user = userService.createUser(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("User created successfully", user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or @userService.isOwner(#id, authentication.name)")
    @Operation(summary = "Update user details, role, status or branch assignment")
    public ApiResponse<UserDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        return ApiResponse.success("User updated successfully", userService.updateUser(id, request, authentication.getName()));
    }

    @PostMapping("/{userId}/branches/{branchId}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Grant additional branch access to a user")
    public ApiResponse<Void> grantBranchAccess(
            @PathVariable Long userId,
            @PathVariable Long branchId,
            Authentication authentication) {

        userService.grantBranchAccess(userId, branchId, authentication.getName());
        return ApiResponse.success("Branch access granted successfully", null);
    }

    @DeleteMapping("/{userId}/branches/{branchId}")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR')")
    @Operation(summary = "Revoke branch access from a user")
    public ApiResponse<Void> revokeBranchAccess(
            @PathVariable Long userId,
            @PathVariable Long branchId,
            Authentication authentication) {

        userService.revokeBranchAccess(userId, branchId, authentication.getName());
        return ApiResponse.success("Branch access revoked successfully", null);
    }

    @GetMapping("/{userId}/branches")
    @PreAuthorize("hasRole('SYSTEM_ADMINISTRATOR') or @userService.isOwner(#userId, authentication.name)")
    @Operation(summary = "Get IDs of all branches accessible by user")
    public ApiResponse<List<Long>> getAccessibleBranchIds(@PathVariable Long userId) {
        return ApiResponse.success(userService.getAccessibleBranchIds(userId));
    }
}

