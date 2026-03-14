package com.company.platform.auth.user.dto.response;

import com.company.platform.auth.role.dto.response.RoleSummary;
import java.time.Instant;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDetailResponse {
  private String id;
  private String username;
  private String email;
  private String fullName;
  private String status;
  private List<RoleSummary> roles;
  private Instant lastLoginAt;
  private Instant createdAt;
  private String createdBy;
  private Instant updatedAt;
  private String updatedBy;
}
