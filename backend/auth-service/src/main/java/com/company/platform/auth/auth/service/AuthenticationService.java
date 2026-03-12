package com.company.platform.auth.auth.service;

import com.company.platform.auth.auth.domain.RefreshToken;
import com.company.platform.auth.auth.dto.request.LoginRequest;
import com.company.platform.auth.auth.dto.request.LogoutRequest;
import com.company.platform.auth.auth.dto.request.RefreshRequest;
import com.company.platform.auth.auth.dto.response.*;
import com.company.platform.auth.auth.repository.RefreshTokenRepository;
import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.repository.AuthUserRepository;
import com.company.platform.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final AuthUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AuthUser user = userRepository.findByUsernameAndDeletedFalse(request.getUsername())
                .orElseThrow(() -> new BusinessException("AUTH_INVALID_CREDENTIALS",
                        "Username or password is invalid", HttpStatus.UNAUTHORIZED));

        if ("LOCKED".equals(user.getStatus())) {
            throw new BusinessException("AUTH_ACCOUNT_LOCKED",
                    "Account is locked due to too many failed login attempts", HttpStatus.UNAUTHORIZED);
        }
        if ("INACTIVE".equals(user.getStatus())) {
            throw new BusinessException("AUTH_ACCOUNT_INACTIVE",
                    "Account is deactivated", HttpStatus.UNAUTHORIZED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
            if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
                user.setStatus("LOCKED");
            }
            userRepository.save(user);
            throw new BusinessException("AUTH_INVALID_CREDENTIALS",
                    "Username or password is invalid", HttpStatus.UNAUTHORIZED);
        }

        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getRoleCode()).toList();
        List<String> claims = user.getRoles().stream()
                .flatMap(r -> r.getClaims().stream())
                .map(c -> c.getClaimCode())
                .distinct().toList();

        String accessToken = tokenProvider.generateAccessToken(user.getId(), roles, claims);
        String refreshTokenValue = tokenProvider.generateRefreshTokenValue();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hashToken(refreshTokenValue))
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(tokenProvider.getRefreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(refreshToken);

        AuthUserSnapshot snapshot = AuthUserSnapshot.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .claims(claims)
                .status(user.getStatus())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(tokenProvider.getAccessTokenExpiry())
                .tokenType("Bearer")
                .user(snapshot)
                .build();
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String hash = hashToken(request.getRefreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new BusinessException("AUTH_TOKEN_REVOKED",
                        "Refresh token is invalid or has been revoked", HttpStatus.UNAUTHORIZED));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("AUTH_TOKEN_EXPIRED",
                    "Refresh token has expired", HttpStatus.UNAUTHORIZED);
        }

        // Revoke old token
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        AuthUser user = userRepository.findByIdAndDeletedFalse(stored.getUserId())
                .orElseThrow(() -> new BusinessException("AUTH_ACCOUNT_INACTIVE",
                        "User account is no longer active", HttpStatus.UNAUTHORIZED));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException("AUTH_ACCOUNT_INACTIVE",
                    "User account is no longer active", HttpStatus.UNAUTHORIZED);
        }

        List<String> roles = user.getRoles().stream()
                .map(r -> r.getRoleCode()).toList();
        List<String> claims = user.getRoles().stream()
                .flatMap(r -> r.getClaims().stream())
                .map(c -> c.getClaimCode())
                .distinct().toList();

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), roles, claims);
        String newRefreshValue = tokenProvider.generateRefreshTokenValue();

        RefreshToken newRefreshToken = RefreshToken.builder()
                .tokenHash(hashToken(newRefreshValue))
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(tokenProvider.getRefreshTokenExpiry()))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return RefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshValue)
                .expiresIn(tokenProvider.getAccessTokenExpiry())
                .tokenType("Bearer")
                .build();
    }

    @Transactional
    public void logout(LogoutRequest request) {
        String hash = hashToken(request.getRefreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
