package com.company.platform.auth.user.domain;

import com.company.platform.shared.entity.FullAuditEntityUUID;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "auth_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthUser extends FullAuditEntityUUID {

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

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "auth_user_role",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  @Builder.Default
  private Set<com.company.platform.auth.role.domain.AuthRole> roles = new HashSet<>();
}
