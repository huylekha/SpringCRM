package com.company.platform.auth.auth.service;

import com.company.platform.auth.user.repository.AuthUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("perm")
public class PermissionEvaluator {

  private final AuthUserRepository userRepository;

  public PermissionEvaluator(AuthUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public boolean has(String permissionCode) {
    String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return userRepository
        .findByIdAndDeletedFalse(userId)
        .map(
            user ->
                user.getRoles().stream()
                    .flatMap(role -> role.getPermissions().stream())
                    .anyMatch(p -> p.getPermissionCode().equals(permissionCode)))
        .orElse(false);
  }

  public String currentUserId() {
    return (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
