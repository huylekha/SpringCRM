package com.company.platform.auth.user.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

/** Domain entity for User (persistence-ignorant). Contains business logic and domain rules. */
@Getter
@Builder(toBuilder = true)
@With
public class User {

  private final String id;
  private final String username;
  private final String email;
  private final String passwordHash;
  private final String status;
  private final String fullName;
  private final Instant lastLoginAt;
  private final int failedLoginAttempts;
  private final Set<String> roleIds; // Simplified - just role IDs for now
  private final String createdBy;
  private final Instant createdAt;
  private final String updatedBy;
  private final Instant updatedAt;

  /** Create a new user with generated ID. */
  public static User create(
      String username,
      String email,
      String passwordHash,
      String fullName,
      String status,
      String createdBy) {
    String userId = UUID.randomUUID().toString();
    Instant now = Instant.now();

    return User.builder()
        .id(userId)
        .username(username)
        .email(email)
        .passwordHash(passwordHash)
        .fullName(fullName)
        .status(status != null ? status : "ACTIVE")
        .failedLoginAttempts(0)
        .roleIds(Set.of()) // Start with no roles
        .createdBy(createdBy)
        .createdAt(now)
        .build();
  }

  /** Update user information (business rule: preserve audit fields). */
  public User updateInfo(String email, String fullName, String updatedBy) {
    return this.toBuilder()
        .email(email != null ? email : this.email)
        .fullName(fullName != null ? fullName : this.fullName)
        .updatedBy(updatedBy)
        .updatedAt(Instant.now())
        .build();
  }

  /** Update user status (business rule: validate status values). */
  public User updateStatus(String newStatus, String updatedBy) {
    if (!isValidStatus(newStatus)) {
      throw new IllegalArgumentException("Invalid status: " + newStatus);
    }

    return this.toBuilder().status(newStatus).updatedBy(updatedBy).updatedAt(Instant.now()).build();
  }

  /** Record failed login attempt (business rule: increment counter). */
  public User recordFailedLogin() {
    int newAttempts = this.failedLoginAttempts + 1;
    String newStatus = newAttempts >= 5 ? "LOCKED" : this.status;

    return this.toBuilder().failedLoginAttempts(newAttempts).status(newStatus).build();
  }

  /** Record successful login (business rule: reset failed attempts). */
  public User recordSuccessfulLogin() {
    return this.toBuilder().failedLoginAttempts(0).lastLoginAt(Instant.now()).build();
  }

  /** Check if the user is active. */
  public boolean isActive() {
    return "ACTIVE".equals(this.status);
  }

  /** Check if the user is locked. */
  public boolean isLocked() {
    return "LOCKED".equals(this.status);
  }

  private boolean isValidStatus(String status) {
    return Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(status);
  }
}
