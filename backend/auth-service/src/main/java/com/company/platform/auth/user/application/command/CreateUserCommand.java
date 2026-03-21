package com.company.platform.auth.user.application.command;

import com.company.platform.shared.cqrs.Command;
import com.company.platform.shared.cqrs.Idempotent;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

/** Command to create a new user. Marked as idempotent to prevent duplicate user creation. */
@Getter
@Builder
@Idempotent(ttlSeconds = 3600) // 1 hour idempotency window
public class CreateUserCommand implements Command<CreateUserResponse> {

  @NotBlank(message = "VALIDATION_USERNAME_REQUIRED")
  @Size(max = 100, message = "VALIDATION_USERNAME_SIZE")
  private final String username;

  @NotBlank(message = "VALIDATION_EMAIL_REQUIRED")
  @Email(message = "VALIDATION_EMAIL_FORMAT")
  @Size(max = 255, message = "VALIDATION_FIELD_SIZE")
  private final String email;

  @NotBlank(message = "VALIDATION_PASSWORD_REQUIRED")
  @Size(min = 8, message = "VALIDATION_PASSWORD_MIN_LENGTH")
  private final String password;

  @NotBlank(message = "VALIDATION_FULLNAME_REQUIRED")
  @Size(max = 200, message = "VALIDATION_FIELD_SIZE")
  private final String fullName;

  private final String status;
}
