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
import com.company.platform.shared.exception.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private static final int MAX_FAILED_ATTEMPTS = 5;

  private final AuthUserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final JwtTokenProvider tokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final MeterRegistry meterRegistry;

  @Transactional
  public LoginResponse login(LoginRequest request) {
    AuthUser user =
        userRepository
            .findByUsernameAndDeletedFalse(request.getUsername())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

    if ("LOCKED".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.AUTH_ACCOUNT_LOCKED, HttpStatus.UNAUTHORIZED);
    }
    if ("INACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.AUTH_ACCOUNT_INACTIVE, HttpStatus.UNAUTHORIZED);
    }

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
      if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
        user.setStatus("LOCKED");
      }
      userRepository.save(user);

      // Track failed login attempts
      Counter.builder("auth.login.attempts")
          .tag("status", "failed")
          .tag("reason", "invalid_credentials")
          .register(meterRegistry)
          .increment();

      throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    user.setFailedLoginAttempts(0);
    user.setLastLoginAt(Instant.now());
    userRepository.save(user);

    // Track successful login
    Counter.builder("auth.login.attempts")
        .tag("status", "success")
        .register(meterRegistry)
        .increment();

    List<String> roles = user.getRoles().stream().map(r -> r.getRoleCode()).toList();
    List<String> claims =
        user.getRoles().stream()
            .flatMap(r -> r.getClaims().stream())
            .map(c -> c.getClaimCode())
            .distinct()
            .toList();

    String accessToken = tokenProvider.generateAccessToken(user.getId(), roles, claims);
    String refreshTokenValue = tokenProvider.generateRefreshTokenValue();

    RefreshToken refreshToken =
        Objects.requireNonNull(
            RefreshToken.builder()
                .tokenHash(hashToken(refreshTokenValue))
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(tokenProvider.getRefreshTokenExpiry()))
                .build());
    refreshTokenRepository.save(refreshToken);

    AuthUserSnapshot snapshot =
        AuthUserSnapshot.builder()
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
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHashAndRevokedFalse(hash)
            .orElseThrow(
                () -> new BusinessException(ErrorCode.AUTH_TOKEN_REVOKED, HttpStatus.UNAUTHORIZED));

    if (stored.getExpiresAt().isBefore(Instant.now())) {
      throw new BusinessException(ErrorCode.AUTH_TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
    }

    // Revoke old token
    stored.setRevoked(true);
    refreshTokenRepository.save(stored);

    AuthUser user =
        userRepository
            .findByIdAndDeletedFalse(stored.getUserId())
            .orElseThrow(
                () ->
                    new BusinessException(
                        ErrorCode.AUTH_ACCOUNT_INACTIVE, HttpStatus.UNAUTHORIZED));

    if (!"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.AUTH_ACCOUNT_INACTIVE, HttpStatus.UNAUTHORIZED);
    }

    List<String> roles = user.getRoles().stream().map(r -> r.getRoleCode()).toList();
    List<String> claims =
        user.getRoles().stream()
            .flatMap(r -> r.getClaims().stream())
            .map(c -> c.getClaimCode())
            .distinct()
            .toList();

    String newAccessToken = tokenProvider.generateAccessToken(user.getId(), roles, claims);
    String newRefreshValue = tokenProvider.generateRefreshTokenValue();

    RefreshToken newRefreshToken =
        Objects.requireNonNull(
            RefreshToken.builder()
                .tokenHash(hashToken(newRefreshValue))
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(tokenProvider.getRefreshTokenExpiry()))
                .build());
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
    refreshTokenRepository
        .findByTokenHashAndRevokedFalse(hash)
        .ifPresent(
            token -> {
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
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }
}
