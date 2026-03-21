package com.company.platform.auth.user.infrastructure.persistence;

import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.domain.User;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain User and JPA AuthUser. Handles the conversion between persistence-ignorant
 * domain objects and JPA entities.
 */
@Component
public class UserMapper {

  /** Convert domain User to JPA AuthUser entity. */
  public AuthUser toJpaEntity(User user) {
    if (user == null) {
      return null;
    }

    AuthUser entity =
        AuthUser.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .passwordHash(user.getPasswordHash())
            .status(user.getStatus())
            .fullName(user.getFullName())
            .lastLoginAt(user.getLastLoginAt())
            .failedLoginAttempts(user.getFailedLoginAttempts())
            .createdBy(user.getCreatedBy())
            .updatedBy(user.getUpdatedBy())
            .build();

    // Set audit fields manually since they might come from domain
    if (user.getCreatedAt() != null) {
      entity.setCreatedAt(user.getCreatedAt());
    }
    if (user.getUpdatedAt() != null) {
      entity.setUpdatedAt(user.getUpdatedAt());
    }

    return entity;
  }

  /** Convert JPA AuthUser entity to domain User. */
  public User toDomainEntity(AuthUser entity) {
    if (entity == null) {
      return null;
    }

    // Extract role IDs from the roles collection
    Set<String> roleIds =
        entity.getRoles() != null
            ? entity.getRoles().stream().map(role -> role.getId()).collect(Collectors.toSet())
            : Set.of();

    return User.builder()
        .id(entity.getId())
        .username(entity.getUsername())
        .email(entity.getEmail())
        .passwordHash(entity.getPasswordHash())
        .status(entity.getStatus())
        .fullName(entity.getFullName())
        .lastLoginAt(entity.getLastLoginAt())
        .failedLoginAttempts(entity.getFailedLoginAttempts())
        .roleIds(roleIds)
        .createdBy(entity.getCreatedBy())
        .createdAt(entity.getCreatedAt())
        .updatedBy(entity.getUpdatedBy())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
