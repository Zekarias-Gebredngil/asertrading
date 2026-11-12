package com.aser.asertrading.identity.service;

import com.aser.asertrading.common.audit.AuditService;
import com.aser.asertrading.common.exception.BadRequestException;
import com.aser.asertrading.common.exception.UnauthorizedException;
import com.aser.asertrading.identity.domain.RefreshToken;
import com.aser.asertrading.identity.domain.Role;
import com.aser.asertrading.identity.domain.User;
import com.aser.asertrading.identity.repository.UserRepository;
import com.aser.asertrading.identity.web.dto.AuthResponse;
import com.aser.asertrading.identity.web.dto.ChangePasswordRequest;
import com.aser.asertrading.identity.web.dto.LoginRequest;
import com.aser.asertrading.identity.web.dto.RefreshTokenRequest;
import com.aser.asertrading.identity.web.dto.RegisterRequest;
import com.aser.asertrading.identity.web.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        User user = userRepository.findByUsernameOrEmail(request.username(), request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!user.isActive()) {
            auditService.log(request.username(), "LOGIN_FAILURE", "User", user.getId().toString(), "Inactive account login attempt", ipAddress);
            throw new UnauthorizedException("User account is inactive. Please contact your system administrator.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), request.password())
            );
        } catch (DisabledException | LockedException e) {
            auditService.log(request.username(), "LOGIN_FAILURE", "User", user.getId().toString(), "Inactive account login attempt", ipAddress);
            throw new UnauthorizedException("User account is inactive. Please contact your system administrator.");
        } catch (Exception e) {
            auditService.log(request.username(), "LOGIN_FAILURE", "User", null, "Failed login attempt", ipAddress);
            throw new UnauthorizedException("Invalid username or password");
        }

        UserPrincipal principal = new UserPrincipal(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("branchId", user.getAssignedBranchId());
        claims.put("email", user.getEmail());

        String accessToken = jwtService.generateToken(principal, claims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditService.log(user.getUsername(), "LOGIN_SUCCESS", "User", user.getId().toString(), "User logged in successfully", ipAddress);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAssignedBranchId()
        );
    }

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username '" + request.username() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email '" + request.email() + "' is already registered");
        }

        User user = User.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .fullName(request.fullName().trim())
                .phone(request.phone() != null ? request.phone().trim() : null)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("branchId", user.getAssignedBranchId());
        claims.put("email", user.getEmail());

        String accessToken = jwtService.generateToken(principal, claims);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditService.log(user.getUsername(), "REGISTER_CUSTOMER", "User", user.getId().toString(), "Customer registered", ipAddress);

        return AuthResponse.of(
                accessToken,
                refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                null
        );
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ipAddress) {
        RefreshToken token = refreshTokenService.verifyToken(request.refreshToken());
        User user = token.getUser();

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive. Please contact your system administrator.");
        }

        // Rotate refresh token: revoke old one and issue a new one
        RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(token);

        UserPrincipal principal = new UserPrincipal(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("branchId", user.getAssignedBranchId());
        claims.put("email", user.getEmail());

        String newAccessToken = jwtService.generateToken(principal, claims);

        auditService.log(user.getUsername(), "TOKEN_REFRESH", "User", user.getId().toString(), "Access token refreshed", ipAddress);

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAssignedBranchId()
        );
    }

    @Transactional
    public void logout(RefreshTokenRequest request, String ipAddress) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            refreshTokenService.revokeToken(request.refreshToken());
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName()
                : "UNKNOWN";
        auditService.log(username, "LOGOUT", "User", null, "User logged out successfully", ipAddress);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UnauthorizedException("No authenticated user found");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + username));

        return UserDto.fromEntity(user);
    }

    @Transactional
    public void changePassword(String username, ChangePasswordRequest request, String ipAddress) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            throw new UnauthorizedException("User must be logged in to change password.");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User not found: " + username));

        if (request.currentPassword() == null || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Invalidate all existing refresh tokens for security
        refreshTokenService.revokeAllUserTokens(user);

        auditService.log(username, "CHANGE_PASSWORD", "User", user.getId().toString(),
                "User changed their password successfully", ipAddress);
    }
}
