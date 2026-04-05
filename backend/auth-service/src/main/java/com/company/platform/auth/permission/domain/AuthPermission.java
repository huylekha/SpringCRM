package com.company.platform.auth.permission.domain;

import com.company.platform.shared.entity.FullAuditEntityUUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthPermission extends FullAuditEntityUUID {

  @Column(name = "permission_code", nullable = false, unique = true, length = 150)
  private String permissionCode;

  @Column(name = "resource_name", nullable = false, length = 80)
  private String resourceName;

  @Column(name = "action_name", nullable = false, length = 80)
  private String actionName;
}
