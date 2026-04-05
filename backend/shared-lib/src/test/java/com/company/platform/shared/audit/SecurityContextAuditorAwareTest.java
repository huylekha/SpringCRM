package com.company.platform.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.platform.shared.security.RequestContext;
import com.company.platform.shared.security.UserContext;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SecurityContextAuditorAware Tests")
class SecurityContextAuditorAwareTest {

  private SecurityContextAuditorAware auditorAware;

  @BeforeEach
  void setUp() {
    auditorAware = new SecurityContextAuditorAware();
    RequestContext.clear();
  }

  @AfterEach
  void tearDown() {
    RequestContext.clear();
  }

  @Test
  @DisplayName("Should return SYSTEM UUID when no RequestContext is set")
  void shouldReturnSystemWhenNoRequestContext() {
    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).isPresent();
    assertThat(auditor.get()).isEqualTo(UserContext.SYSTEM.userId().toString());
  }

  @Test
  @DisplayName("Should return user ID from RequestContext when set")
  void shouldReturnUserIdFromRequestContext() {
    UUID userId = UUID.randomUUID();
    RequestContext.set(new UserContext(userId, "John Doe", "tenant-1"));

    Optional<String> auditor = auditorAware.getCurrentAuditor();

    assertThat(auditor).isPresent();
    assertThat(auditor.get()).isEqualTo(userId.toString());
  }

  @Test
  @DisplayName("Should return current auditor synchronously from RequestContext")
  void shouldReturnCurrentAuditorSynchronously() {
    UUID userId = UUID.randomUUID();
    RequestContext.set(new UserContext(userId, "Jane Doe", "tenant-2"));

    String auditor = auditorAware.getCurrentAuditorSync();

    assertThat(auditor).isEqualTo(userId.toString());
  }

  @Test
  @DisplayName("Should return SYSTEM UUID synchronously when no RequestContext")
  void shouldReturnSystemSynchronouslyWhenNoRequestContext() {
    String auditor = auditorAware.getCurrentAuditorSync();

    assertThat(auditor).isEqualTo(UserContext.SYSTEM.userId().toString());
  }

  @Test
  @DisplayName("Should return true for hasAuthenticatedUser when real user is set")
  void shouldReturnTrueForHasAuthenticatedUserWhenRealUserSet() {
    UUID userId = UUID.randomUUID();
    RequestContext.set(new UserContext(userId, "Real User", "tenant-1"));

    assertThat(auditorAware.hasAuthenticatedUser()).isTrue();
  }

  @Test
  @DisplayName("Should return false for hasAuthenticatedUser when no RequestContext")
  void shouldReturnFalseForHasAuthenticatedUserWhenNoRequestContext() {
    assertThat(auditorAware.hasAuthenticatedUser()).isFalse();
  }

  @Test
  @DisplayName("Should return false for hasAuthenticatedUser when SYSTEM user")
  void shouldReturnFalseForHasAuthenticatedUserWhenSystemUser() {
    RequestContext.set(UserContext.SYSTEM);

    assertThat(auditorAware.hasAuthenticatedUser()).isFalse();
  }

  @Test
  @DisplayName("Should clear correctly between requests")
  void shouldClearCorrectlyBetweenRequests() {
    UUID userId = UUID.randomUUID();
    RequestContext.set(new UserContext(userId, "User A", "tenant-a"));

    assertThat(auditorAware.getCurrentAuditor().orElseThrow()).isEqualTo(userId.toString());
    assertThat(auditorAware.hasAuthenticatedUser()).isTrue();

    RequestContext.clear();

    assertThat(auditorAware.getCurrentAuditor().orElseThrow())
        .isEqualTo(UserContext.SYSTEM.userId().toString());
    assertThat(auditorAware.hasAuthenticatedUser()).isFalse();
  }
}
