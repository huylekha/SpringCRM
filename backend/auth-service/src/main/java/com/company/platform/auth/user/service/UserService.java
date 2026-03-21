package com.company.platform.auth.user.service;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.auth.role.dto.response.RoleSummary;
import com.company.platform.auth.role.repository.AuthRoleRepository;
import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.dto.request.*;
import com.company.platform.auth.user.dto.response.*;
import com.company.platform.auth.user.repository.AuthUserRepository;
import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.exception.ErrorCode;
import com.company.platform.shared.exception.ResourceNotFoundException;
import com.company.platform.shared.response.PageResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final AuthUserRepository userRepository;
  private final AuthRoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final PermissionEvaluator permissionEvaluator;
  private final MeterRegistry meterRegistry;

  @Transactional
  public UserResponse createUser(CreateUserRequest request) {
    if (userRepository.existsByUsernameAndDeletedFalse(request.getUsername())) {
      throw new DuplicateResourceException(ErrorCode.USER_ALREADY_EXISTS);
    }
    if (userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
      throw new DuplicateResourceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
    }

    AuthUser user =
        AuthUser.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName())
            .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
            .createdBy(permissionEvaluator.currentUserId())
            .build();
    user = userRepository.save(user);

    // Track user registration
    Counter.builder("user.registrations")
        .tag("status", user.getStatus())
        .register(meterRegistry)
        .increment();

    return toUserResponse(user);
  }

  @Transactional(readOnly = true)
  public UserDetailResponse getUser(String id) {
    AuthUser user = findActiveUser(id);
    return toUserDetailResponse(user);
  }

  @Transactional
  public UserResponse updateUser(String id, UpdateUserRequest request) {
    AuthUser user = findActiveUser(id);
    if (request.getEmail() != null) {
      if (!request.getEmail().equals(user.getEmail())
          && userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
        throw new DuplicateResourceException(ErrorCode.USER_EMAIL_ALREADY_EXISTS);
      }
      user.setEmail(request.getEmail());
    }
    if (request.getFullName() != null) {
      user.setFullName(request.getFullName());
    }
    user.setUpdatedBy(permissionEvaluator.currentUserId());
    user = userRepository.save(user);
    return toUserResponse(user);
  }

  @Transactional
  public UserResponse updateStatus(String id, StatusUpdateRequest request) {
    AuthUser user = findActiveUser(id);
    String newStatus = request.getStatus().toUpperCase();
    if (!Set.of("ACTIVE", "INACTIVE", "LOCKED").contains(newStatus)) {
      throw new BusinessException(ErrorCode.VALIDATION_INVALID_VALUE, HttpStatus.valueOf(422));
    }
    if ("INACTIVE".equals(newStatus) && "SUPER_ADMIN".equals(getHighestRole(user))) {
      long adminCount = userRepository.countByRoles_RoleCodeAndDeletedFalse("SUPER_ADMIN");
      if (adminCount <= 1) {
        throw new BusinessException(ErrorCode.AUTH_LAST_ADMIN_PROTECTED, HttpStatus.valueOf(422));
      }
    }
    user.setStatus(newStatus);
    user.setUpdatedBy(permissionEvaluator.currentUserId());
    user = userRepository.save(user);

    // Track status changes
    Counter.builder("user.status.changes")
        .tag("new_status", newStatus)
        .register(meterRegistry)
        .increment();

    return toUserResponse(user);
  }

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> listUsers(String status, Pageable pageable) {
    Page<AuthUser> page;
    if (status != null && !status.isBlank()) {
      page = userRepository.findAllByStatusAndDeletedFalse(status.toUpperCase(), pageable);
    } else {
      page = userRepository.findAllByDeletedFalse(pageable);
    }
    return toPageResponse(page);
  }

  @Transactional
  public AssignRolesResponse assignRoles(String userId, AssignRolesRequest request) {
    AuthUser user = findActiveUser(userId);
    String currentUserId = permissionEvaluator.currentUserId();

    List<AuthRole> rolesToAssign =
        request.getRoleIds().stream()
            .map(
                roleId ->
                    roleRepository
                        .findByIdAndDeletedFalse(roleId)
                        .orElseThrow(
                            () -> new ResourceNotFoundException(ErrorCode.AUTH_ROLE_NOT_FOUND)))
            .toList();

    boolean assignsSuperAdmin =
        rolesToAssign.stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
    if (assignsSuperAdmin) {
      AuthUser actor = userRepository.findByIdAndDeletedFalse(currentUserId).orElse(null);
      boolean actorIsSuperAdmin =
          actor != null
              && actor.getRoles().stream().anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleCode()));
      if (!actorIsSuperAdmin) {
        throw new BusinessException(ErrorCode.AUTH_INSUFFICIENT_PERMISSION, HttpStatus.FORBIDDEN);
      }
    }

    user.getRoles().addAll(rolesToAssign);
    user.setUpdatedBy(currentUserId);
    user = userRepository.save(user);

    List<RoleSummary> summaries =
        user.getRoles().stream()
            .map(
                r ->
                    RoleSummary.builder()
                        .id(r.getId())
                        .roleCode(r.getRoleCode())
                        .roleName(r.getRoleName())
                        .build())
            .toList();

    return AssignRolesResponse.builder()
        .userId(user.getId())
        .roles(summaries)
        .assignedAt(Instant.now())
        .assignedBy(currentUserId)
        .build();
  }

  @Transactional
  public void removeRole(String userId, String roleId) {
    AuthUser user = findActiveUser(userId);
    user.getRoles().removeIf(r -> r.getId().equals(roleId));
    user.setUpdatedBy(permissionEvaluator.currentUserId());
    userRepository.save(user);
  }

  private AuthUser findActiveUser(String id) {
    return userRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.USER_NOT_FOUND));
  }

  private String getHighestRole(AuthUser user) {
    return user.getRoles().stream()
        .map(AuthRole::getRoleCode)
        .filter("SUPER_ADMIN"::equals)
        .findFirst()
        .orElse(null);
  }

  private UserResponse toUserResponse(AuthUser u) {
    return UserResponse.builder()
        .id(u.getId())
        .username(u.getUsername())
        .email(u.getEmail())
        .fullName(u.getFullName())
        .status(u.getStatus())
        .createdAt(u.getCreatedAt())
        .createdBy(u.getCreatedBy())
        .build();
  }

  private UserDetailResponse toUserDetailResponse(AuthUser u) {
    List<RoleSummary> roles =
        u.getRoles().stream()
            .map(
                r ->
                    RoleSummary.builder()
                        .id(r.getId())
                        .roleCode(r.getRoleCode())
                        .roleName(r.getRoleName())
                        .build())
            .toList();
    return UserDetailResponse.builder()
        .id(u.getId())
        .username(u.getUsername())
        .email(u.getEmail())
        .fullName(u.getFullName())
        .status(u.getStatus())
        .roles(roles)
        .lastLoginAt(u.getLastLoginAt())
        .createdAt(u.getCreatedAt())
        .createdBy(u.getCreatedBy())
        .updatedAt(u.getUpdatedAt())
        .updatedBy(u.getUpdatedBy())
        .build();
  }

  private PageResponse<UserResponse> toPageResponse(Page<AuthUser> page) {
    List<UserResponse> content = page.getContent().stream().map(this::toUserResponse).toList();
    return PageResponse.<UserResponse>builder()
        .content(content)
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .build();
  }
}
