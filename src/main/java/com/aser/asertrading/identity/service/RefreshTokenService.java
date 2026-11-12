package com.aser.asertrading.identity.service;

import com.aser.asertrading.common.exception.UnauthorizedException;
import com.aser.asertrading.identity.domain.RefreshToken;
import com.aser.asertrading.identity.domain.User;
import com.aser.asertrading.identity.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name());
        claims.put("branchId", user.getAssignedBranchId());
        claims.put("email", user.getEmail());

        String jwtRefreshToken = jwtService.generateRefreshToken(principal, claims);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(jwtRefreshToken)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional(readOnly = true)
    public RefreshToken verifyToken(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.isRevoked()) {
            throw new UnauthorizedException("Refresh token has been revoked. Please log in again.");
        }

        if (token.getExpiryDate().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }

        return token;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        return createRefreshToken(oldToken.getUser());
    }

    @Transactional
    public void revokeToken(String tokenStr) {
        if (tokenStr == null || tokenStr.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(tokenStr).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.info("Revoked refresh token for user {}", token.getUser().getUsername());
        });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
        log.info("Revoked all active refresh tokens for user {}", user.getUsername());
    }
}

