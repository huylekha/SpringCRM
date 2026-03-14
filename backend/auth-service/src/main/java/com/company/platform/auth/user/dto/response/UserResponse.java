package com.company.platform.auth.user.dto.response;

import java.time.Instant;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
  private String id;
  private String username;
  private String email;
  private String fullName;
  private String status;
  private Instant createdAt;
  private String createdBy;
}
