package com.company.platform.auth.role.domain;

import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.auth.permission.domain.AuthPermission;
import com.company.platform.shared.entity.FullAuditEntityUUID;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "auth_role")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthRole extends FullAuditEntityUUID {

  @Column(name = "role_code", nullable = false, unique = true, length = 80)
  private String roleCode;

  @Column(name = "role_name", nullable = false, length = 120)
  private String roleName;

  @Column(length = 300)
  private String description;

  @Column(name = "is_seed", nullable = false)
  @Builder.Default
  private boolean seed = false;

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
}
