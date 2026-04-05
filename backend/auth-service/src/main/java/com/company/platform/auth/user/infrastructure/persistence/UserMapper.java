package com.company.platform.auth.user.infrastructure.persistence;

import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.domain.User;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

  public AuthUser toJpaEntity(User user) {
    if (user == null) {
      return null;
    }

    AuthUser entity =
        AuthUser.builder()
            .username(user.getUsername())
            .email(user.getEmail())
            .passwordHash(user.getPasswordHash())
            .status(user.getStatus())
            .fullName(user.getFullName())
            .lastLoginAt(user.getLastLoginAt())
            .failedLoginAttempts(user.getFailedLoginAttempts())
            .build();

    if (user.getId() != null) {
      entity.setId(UUID.fromString(user.getId()));
    }
    if (user.getCreatedAt() != null) {
      entity.setCreatedAt(user.getCreatedAt());
    }
    if (user.getUpdatedAt() != null) {
      entity.setUpdatedAt(user.getUpdatedAt());
    }

    return entity;
  }

  public User toDomainEntity(AuthUser entity) {
    if (entity == null) {
      return null;
    }

    Set<String> roleIds =
        entity.getRoles() != null
            ? entity.getRoles().stream()
                .map(role -> role.getId().toString())
                .collect(Collectors.toSet())
            : Set.of();

    return User.builder()
        .id(entity.getId() != null ? entity.getId().toString() : null)
        .username(entity.getUsername())
        .email(entity.getEmail())
        .passwordHash(entity.getPasswordHash())
        .status(entity.getStatus())
        .fullName(entity.getFullName())
        .lastLoginAt(entity.getLastLoginAt())
        .failedLoginAttempts(entity.getFailedLoginAttempts())
        .roleIds(roleIds)
        .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null)
        .createdAt(entity.getCreatedAt())
        .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().toString() : null)
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
