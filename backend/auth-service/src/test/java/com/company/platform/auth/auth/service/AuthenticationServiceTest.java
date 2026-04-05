package com.company.platform.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.platform.auth.auth.domain.RefreshToken;
import com.company.platform.auth.auth.dto.request.LoginRequest;
import com.company.platform.auth.auth.dto.request.LogoutRequest;
import com.company.platform.auth.auth.dto.request.RefreshRequest;
import com.company.platform.auth.auth.dto.response.LoginResponse;
import com.company.platform.auth.auth.dto.response.RefreshResponse;
import com.company.platform.auth.auth.repository.RefreshTokenRepository;
import com.company.platform.auth.config.JwtProperties;
import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.repository.AuthUserRepository;
import com.company.platform.shared.exception.BusinessException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ROLE_ID = UUID.randomUUID();
  private static final UUID REFRESH_TOKEN_ID = UUID.randomUUID();

  @Mock private AuthUserRepository userRepository;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  private JwtTokenProvider tokenProvider;
  private PasswordEncoder passwordEncoder;
  private MeterRegistry meterRegistry;
  private AuthenticationService authenticationService;

  @BeforeEach
  void setUp() {
    JwtProperties props = new JwtProperties();
    props.setIssuer("test");
    props.setSecret("test-jwt-secret-key-for-unit-tests-must-be-at-least-64-characters-long!!!!!");
    props.setAccessTokenExpiry(900);
    props.setRefreshTokenExpiry(604800);
    tokenProvider = new JwtTokenProvider(props);
    passwordEncoder = new BCryptPasswordEncoder();
    meterRegistry = new SimpleMeterRegistry();
    authenticationService =
        new AuthenticationService(
            userRepository, refreshTokenRepository, tokenProvider, passwordEncoder, meterRegistry);
  }

  private AuthRole buildRole(UUID id, String code, String name) {
    AuthRole role = AuthRole.builder().roleCode(code).roleName(name).build();
    role.setId(id);
    return role;
  }

  private AuthUser buildUser(
      UUID id, String username, String email, String hash, String status, Set<AuthRole> roles) {
    AuthUser user =
        AuthUser.builder()
            .username(username)
            .email(email)
            .passwordHash(hash)
            .status(status)
            .roles(roles)
            .build();
    user.setId(id);
    return user;
  }

  private RefreshToken buildRefreshToken(UUID id, UUID userId, boolean revoked, Instant expiresAt) {
    RefreshToken token =
        RefreshToken.builder().userId(userId).revoked(revoked).expiresAt(expiresAt).build();
    token.setId(id);
    return token;
  }

  @Test
  void login_validCredentials_returnsTokens() {
    AuthRole role = buildRole(ROLE_ID, "SUPER_ADMIN", "Super Admin");
    AuthUser user =
        buildUser(
            USER_ID,
            "admin",
            "a@b.com",
            passwordEncoder.encode("Admin@123456"),
            "ACTIVE",
            Set.of(role));

    when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenReturn(user);
    when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    LoginRequest req = new LoginRequest();
    req.setUsername("admin");
    req.setPassword("Admin@123456");

    LoginResponse resp = authenticationService.login(req);
    assertThat(resp.getAccessToken()).isNotBlank();
    assertThat(resp.getRefreshToken()).isNotBlank();
    assertThat(resp.getTokenType()).isEqualTo("Bearer");
    assertThat(resp.getUser().getUsername()).isEqualTo("admin");
  }

  @Test
  void login_wrongPassword_throwsUnauthorized() {
    AuthUser user =
        buildUser(
            USER_ID, "admin", null, passwordEncoder.encode("Admin@123456"), "ACTIVE", Set.of());

    when(userRepository.findByUsernameAndDeletedFalse("admin")).thenReturn(Optional.of(user));
    when(userRepository.save(any())).thenReturn(user);

    LoginRequest req = new LoginRequest();
    req.setUsername("admin");
    req.setPassword("WrongPassword");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "AUTH_001");
  }

  @Test
  void login_lockedAccount_throwsLocked() {
    AuthUser user = buildUser(USER_ID, "locked", null, "hash", "LOCKED", Set.of());

    when(userRepository.findByUsernameAndDeletedFalse("locked")).thenReturn(Optional.of(user));

    LoginRequest req = new LoginRequest();
    req.setUsername("locked");
    req.setPassword("any");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "AUTH_009");
  }

  @Test
  void login_nonexistentUser_throwsInvalidCredentials() {
    when(userRepository.findByUsernameAndDeletedFalse("ghost")).thenReturn(Optional.empty());

    LoginRequest req = new LoginRequest();
    req.setUsername("ghost");
    req.setPassword("any");

    assertThatThrownBy(() -> authenticationService.login(req))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "AUTH_001");
  }

  @Test
  void refresh_validToken_returnsNewTokens() {
    RefreshToken stored =
        buildRefreshToken(REFRESH_TOKEN_ID, USER_ID, false, Instant.now().plusSeconds(3600));

    AuthRole role = buildRole(ROLE_ID, "SALES_REP", "Sales Rep");
    AuthUser user = buildUser(USER_ID, "user1", null, null, "ACTIVE", Set.of(role));

    String refreshValue = "test-refresh-value";
    when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
        .thenReturn(Optional.of(stored));
    when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user));

    RefreshRequest req = new RefreshRequest();
    req.setRefreshToken(refreshValue);

    RefreshResponse resp = authenticationService.refresh(req);
    assertThat(resp.getAccessToken()).isNotBlank();
    assertThat(resp.getRefreshToken()).isNotBlank();
    assertThat(stored.isRevoked()).isTrue();
  }

  @Test
  void logout_revokesToken() {
    RefreshToken stored =
        buildRefreshToken(REFRESH_TOKEN_ID, USER_ID, false, Instant.now().plusSeconds(3600));

    when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any()))
        .thenReturn(Optional.of(stored));
    when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

    LogoutRequest req = new LogoutRequest();
    req.setRefreshToken("some-token");
    authenticationService.logout(req);
    assertThat(stored.isRevoked()).isTrue();
  }
}
