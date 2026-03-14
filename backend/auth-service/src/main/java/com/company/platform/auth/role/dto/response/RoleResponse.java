package com.company.platform.auth.role.dto.response;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleResponse {
  private String id;
  private String roleCode;
  private String roleName;
  private String description;
  private Instant createdAt;
  private String createdBy;
}
