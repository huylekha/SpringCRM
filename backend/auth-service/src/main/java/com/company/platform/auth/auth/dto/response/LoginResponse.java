package com.company.platform.auth.auth.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
  private String accessToken;
  private String refreshToken;
  private int expiresIn;
  private String tokenType;
  private AuthUserSnapshot user;
}
