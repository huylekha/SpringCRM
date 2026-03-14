package com.company.platform.auth.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "auth_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser {

  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 255)
  private String passwordHash;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "full_name", length = 200)
  private String fullName;

  @Column(name = "last_login_at")
  private Instant lastLoginAt;

  @Column(name = "failed_login_attempts")
  @Builder.Default
  private int failedLoginAttempts = 0;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by", nullable = false, updatable = false, length = 36)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "updated_at")
  private Instant updatedAt;

  @Column(name = "updated_by", length = 36)
  private String updatedBy;

  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @Column(name = "deleted_at")
  private Instant deletedAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "auth_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  @Builder.Default
  private Set<com.company.platform.auth.role.domain.AuthRole> roles = new HashSet<>();

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
  }
}
