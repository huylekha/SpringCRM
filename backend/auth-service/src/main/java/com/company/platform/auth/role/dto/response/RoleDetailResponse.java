package com.company.platform.auth.role.dto.response;

import com.company.platform.auth.claim.dto.response.ClaimResponse;
import com.company.platform.auth.permission.dto.response.PermissionResponse;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleDetailResponse {
  private String id;
  private String roleCode;
  private String roleName;
  private String description;
  private List<ClaimResponse> claims;
  private List<PermissionResponse> permissions;
  private Instant createdAt;
  private Instant updatedAt;
}
