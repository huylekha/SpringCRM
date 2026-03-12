package com.company.platform.auth.auth.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock private AuthUserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    private JwtTokenProvider tokenProvider;
    private PasswordEncoder passwordEncoder;
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
        authenticationService = new AuthenticationService(
                userRepository, refreshTokenRepository, tokenProvider, passwordEncoder);
    }

    @Test
    void login_validCredentials_returnsTokens() {
        AuthRole role = AuthRole.builder().id("r1").roleCode("SUPER_ADMIN").roleName("Super Admin").build();
        AuthUser user = AuthUser.builder()
                .id("u1").username("admin").email("a@b.com")
                .passwordHash(passwordEncoder.encode("Admin@123456"))
                .status("ACTIVE").roles(Set.of(role))
                .build();
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
        AuthUser user = AuthUser.builder()
                .id("u1").username("admin")
                .passwordHash(passwordEncoder.encode("Admin@123456"))
                .status("ACTIVE").roles(Set.of())
                .build();
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
        AuthUser user = AuthUser.builder()
                .id("u1").username("locked")
                .passwordHash("hash").status("LOCKED")
                .roles(Set.of())
                .build();
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
        RefreshToken stored = RefreshToken.builder()
                .id("rt1").userId("u1").revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        AuthRole role = AuthRole.builder().id("r1").roleCode("SALES_REP").roleName("Sales Rep").build();
        AuthUser user = AuthUser.builder()
                .id("u1").username("user1").status("ACTIVE").roles(Set.of(role))
                .build();

        String refreshValue = "test-refresh-value";
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findByIdAndDeletedFalse("u1")).thenReturn(Optional.of(user));

        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken(refreshValue);

        RefreshResponse resp = authenticationService.refresh(req);
        assertThat(resp.getAccessToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        assertThat(stored.isRevoked()).isTrue();
    }

    @Test
    void logout_revokesToken() {
        RefreshToken stored = RefreshToken.builder()
                .id("rt1").userId("u1").revoked(false)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByTokenHashAndRevokedFalse(any())).thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        LogoutRequest req = new LogoutRequest();
        req.setRefreshToken("some-token");
        authenticationService.logout(req);
        assertThat(stored.isRevoked()).isTrue();
    }
}
