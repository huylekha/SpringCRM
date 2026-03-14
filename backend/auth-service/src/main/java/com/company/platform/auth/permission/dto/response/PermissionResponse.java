package com.company.platform.auth.permission.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {
  private String id;
  private String permissionCode;
  private String resourceName;
  private String actionName;
}
