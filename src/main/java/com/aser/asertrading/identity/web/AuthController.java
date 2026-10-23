package com.aser.asertrading.identity.web;

import com.aser.asertrading.common.dto.ApiResponse;
import com.aser.asertrading.identity.service.AuthService;
import com.aser.asertrading.identity.web.dto.AuthResponse;
import com.aser.asertrading.identity.web.dto.LoginRequest;
import com.aser.asertrading.identity.web.dto.RegisterRequest;
import com.aser.asertrading.identity.web.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for login, customer registration and session verification")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user (staff or customer) and retrieve JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse response = authService.registerCustomer(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Customer registration successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser() {
        return ResponseEntity.ok(ApiResponse.success(authService.getCurrentUser()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password for the authenticated user")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody com.aser.asertrading.identity.web.dto.ChangePasswordRequest request,
            org.springframework.security.core.Authentication authentication,
            HttpServletRequest httpRequest) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.aser.asertrading.common.exception.UnauthorizedException("User must be authenticated to change password");
        }
        String clientIp = httpRequest.getRemoteAddr();
        authService.changePassword(authentication.getName(), request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }
}

