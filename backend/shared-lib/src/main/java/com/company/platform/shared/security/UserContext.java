package com.company.platform.shared.security;

import java.util.UUID;

public record UserContext(UUID userId, String fullName, String tenantId) {
  public static final UserContext SYSTEM = new UserContext(new UUID(0, 0), "SYSTEM", "SYSTEM");

  public boolean isSystem() {
    return SYSTEM.userId().equals(this.userId);
  }
}
