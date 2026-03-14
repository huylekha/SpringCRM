package com.company.platform.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {
  private String issuer = "crm-platform";
  private String secret;
  private int accessTokenExpiry = 900;
  private int refreshTokenExpiry = 604800;
}
