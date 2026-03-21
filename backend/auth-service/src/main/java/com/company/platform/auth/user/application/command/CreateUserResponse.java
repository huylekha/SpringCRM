package com.company.platform.auth.user.application.command;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/** Response from creating a user. */
@Getter
@Builder
public class CreateUserResponse {

  private final String id;
  private final String username;
  private final String email;
  private final String fullName;
  private final String status;
  private final Instant createdAt;
  private final String createdBy;
}
