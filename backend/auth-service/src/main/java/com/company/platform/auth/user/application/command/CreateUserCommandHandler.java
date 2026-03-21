package com.company.platform.auth.user.application.command;

import com.company.platform.auth.user.domain.User;
import com.company.platform.auth.user.domain.UserRepository;
import com.company.platform.shared.cqrs.CommandHandler;
import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Handler for CreateUserCommand. Creates a new user with validation and business rules. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateUserCommandHandler
    implements CommandHandler<CreateUserCommand, CreateUserResponse> {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public CreateUserResponse handle(CreateUserCommand command) {
    log.info("Creating user: username={}, email={}", command.getUsername(), command.getEmail());

    // Business rule: Check for duplicate username
    if (userRepository.existsByUsername(command.getUsername())) {
      throw new BusinessException(ErrorCode.USER_USERNAME_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    // Business rule: Check for duplicate email
    if (userRepository.existsByEmail(command.getEmail())) {
      throw new BusinessException(ErrorCode.USER_EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT);
    }

    // Create domain user
    String createdBy = getCurrentUserId();
    String passwordHash = passwordEncoder.encode(command.getPassword());

    User user =
        User.create(
            command.getUsername(),
            command.getEmail(),
            passwordHash,
            command.getFullName(),
            command.getStatus(),
            createdBy);

    // Save user
    User savedUser = userRepository.save(user);

    log.info(
        "User created successfully: userId={}, username={}",
        savedUser.getId(),
        savedUser.getUsername());

    return CreateUserResponse.builder()
        .id(savedUser.getId())
        .username(savedUser.getUsername())
        .email(savedUser.getEmail())
        .fullName(savedUser.getFullName())
        .status(savedUser.getStatus())
        .createdAt(savedUser.getCreatedAt())
        .createdBy(savedUser.getCreatedBy())
        .build();
  }

  @Override
  public Class<CreateUserCommand> getCommandType() {
    return CreateUserCommand.class;
  }

  private String getCurrentUserId() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.debug("Could not get current user ID: {}", e.getMessage());
    }
    return "system";
  }
}
