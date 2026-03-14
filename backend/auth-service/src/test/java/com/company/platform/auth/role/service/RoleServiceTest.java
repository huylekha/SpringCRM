package com.company.platform.auth.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.claim.repository.AuthClaimRepository;
import com.company.platform.auth.permission.repository.AuthPermissionRepository;
import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.auth.role.dto.request.CreateRoleRequest;
import com.company.platform.auth.role.dto.response.RoleResponse;
import com.company.platform.auth.role.repository.AuthRoleRepository;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.exception.ResourceNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private AuthRoleRepository roleRepository;
  @Mock private AuthClaimRepository claimRepository;
  @Mock private AuthPermissionRepository permissionRepository;
  @Mock private PermissionEvaluator permissionEvaluator;
  private RoleService roleService;

  @BeforeEach
  void setUp() {
    roleService =
        new RoleService(roleRepository, claimRepository, permissionRepository, permissionEvaluator);
  }

  @Test
  void createRole_success() {
    when(roleRepository.existsByRoleCodeAndDeletedFalse("NEW_ROLE")).thenReturn(false);
    when(permissionEvaluator.currentUserId()).thenReturn("admin-id");
    when(roleRepository.save(any()))
        .thenAnswer(
            i -> {
              AuthRole r = i.getArgument(0);
              r.setId("generated-id");
              return r;
            });

    CreateRoleRequest req = new CreateRoleRequest();
    req.setRoleCode("NEW_ROLE");
    req.setRoleName("New Role");
    req.setDescription("Test");

    RoleResponse resp = roleService.createRole(req);
    assertThat(resp.getRoleCode()).isEqualTo("NEW_ROLE");
  }

  @Test
  void createRole_duplicate_throws409() {
    when(roleRepository.existsByRoleCodeAndDeletedFalse("EXISTING")).thenReturn(true);

    CreateRoleRequest req = new CreateRoleRequest();
    req.setRoleCode("EXISTING");
    req.setRoleName("Existing");

    assertThatThrownBy(() -> roleService.createRole(req))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void getRole_notFound_throws404() {
    when(roleRepository.findByIdAndDeletedFalse("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleService.getRole("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
