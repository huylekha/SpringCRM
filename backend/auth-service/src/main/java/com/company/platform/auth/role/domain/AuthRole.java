package com.company.platform.auth.role.domain;

import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.auth.permission.domain.AuthPermission;
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
@Table(name = "auth_role")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRole {

  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "role_code", nullable = false, unique = true, length = 80)
  private String roleCode;

  @Column(name = "role_name", nullable = false, length = 120)
  private String roleName;

  @Column(length = 300)
  private String description;

  @Column(name = "is_seed", nullable = false)
  @Builder.Default
  private boolean seed = false;

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
      name = "auth_role_claim",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "claim_id"))
  @Builder.Default
  private Set<AuthClaim> claims = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "auth_role_permission",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  @Builder.Default
  private Set<AuthPermission> permissions = new HashSet<>();

  @PrePersist
  public void prePersist() {
    if (this.id == null) {
      this.id = UUID.randomUUID().toString();
    }
  }
}
