package com.company.platform.auth.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.platform.auth.config.JwtProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

  private JwtTokenProvider tokenProvider;

  @BeforeEach
  void setUp() {
    JwtProperties props = new JwtProperties();
    props.setIssuer("test-issuer");
    props.setSecret("test-jwt-secret-key-for-unit-tests-must-be-at-least-64-characters-long!!!!!");
    props.setAccessTokenExpiry(900);
    props.setRefreshTokenExpiry(604800);
    tokenProvider = new JwtTokenProvider(props);
  }

  @Test
  void generateAccessToken_validPayload_returnsToken() {
    String token =
        tokenProvider.generateAccessToken(
            "user-123", List.of("SUPER_ADMIN"), List.of("admin_claim"));
    assertThat(token).isNotBlank();
    assertThat(tokenProvider.isTokenValid(token)).isTrue();
  }

  @Test
  void getUserIdFromToken_returnsSubject() {
    String token = tokenProvider.generateAccessToken("user-456", List.of("SALES_REP"), List.of());
    assertThat(tokenProvider.getUserIdFromToken(token)).isEqualTo("user-456");
  }

  @Test
  void getRolesFromToken_returnsRoles() {
    String token =
        tokenProvider.generateAccessToken("user-789", List.of("CRM_ADMIN", "AUDITOR"), List.of());
    List<String> roles = tokenProvider.getRolesFromToken(token);
    assertThat(roles).containsExactlyInAnyOrder("CRM_ADMIN", "AUDITOR");
  }

  @Test
  void isTokenValid_invalidToken_returnsFalse() {
    assertThat(tokenProvider.isTokenValid("invalid.token.here")).isFalse();
  }

  @Test
  void isTokenValid_nullToken_returnsFalse() {
    assertThat(tokenProvider.isTokenValid(null)).isFalse();
  }

  @Test
  void generateRefreshTokenValue_returnsUniqueValues() {
    String t1 = tokenProvider.generateRefreshTokenValue();
    String t2 = tokenProvider.generateRefreshTokenValue();
    assertThat(t1).isNotEqualTo(t2);
  }
}
