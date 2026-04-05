package com.company.platform.auth.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.auth.role.repository.AuthRoleRepository;
import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.auth.user.dto.request.CreateUserRequest;
import com.company.platform.auth.user.dto.request.StatusUpdateRequest;
import com.company.platform.auth.user.dto.response.UserResponse;
import com.company.platform.auth.user.repository.AuthUserRepository;
import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.DuplicateResourceException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ROLE_ID = UUID.randomUUID();

  @Mock private AuthUserRepository userRepository;
  @Mock private AuthRoleRepository roleRepository;
  @Mock private PermissionEvaluator permissionEvaluator;
  private PasswordEncoder passwordEncoder;
  private MeterRegistry meterRegistry;
  private UserService userService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    meterRegistry = new SimpleMeterRegistry();
    userService =
        new UserService(
            userRepository, roleRepository, passwordEncoder, permissionEvaluator, meterRegistry);
  }

  @Test
  void createUser_success() {
    when(userRepository.existsByUsernameAndDeletedFalse("newuser")).thenReturn(false);
    when(userRepository.existsByEmailAndDeletedFalse("new@test.com")).thenReturn(false);
    when(userRepository.save(any()))
        .thenAnswer(
            i -> {
              AuthUser u = i.getArgument(0);
              u.setId(UUID.randomUUID());
              return u;
            });

    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("newuser");
    req.setEmail("new@test.com");
    req.setPassword("SecurePass1!");
    req.setFullName("New User");

    UserResponse resp = userService.createUser(req);
    assertThat(resp.getUsername()).isEqualTo("newuser");
    assertThat(resp.getEmail()).isEqualTo("new@test.com");
    verify(userRepository).save(any(AuthUser.class));
  }

  @Test
  void createUser_duplicateUsername_throws409() {
    when(userRepository.existsByUsernameAndDeletedFalse("existing")).thenReturn(true);

    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("existing");
    req.setEmail("e@test.com");
    req.setPassword("Pass1234!");
    req.setFullName("Existing");

    assertThatThrownBy(() -> userService.createUser(req))
        .isInstanceOf(DuplicateResourceException.class)
        .hasFieldOrPropertyWithValue("code", "USER_101");
  }

  @Test
  void updateStatus_lastSuperAdmin_preventDeactivation() {
    AuthRole superAdmin = AuthRole.builder().roleCode("SUPER_ADMIN").roleName("SA").build();
    superAdmin.setId(ROLE_ID);

    AuthUser user =
        AuthUser.builder().username("admin").status("ACTIVE").roles(Set.of(superAdmin)).build();
    user.setId(USER_ID);

    when(userRepository.findByIdAndDeletedFalse(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.countByRoles_RoleCodeAndDeletedFalse("SUPER_ADMIN")).thenReturn(1L);

    StatusUpdateRequest req = new StatusUpdateRequest();
    req.setStatus("INACTIVE");

    assertThatThrownBy(() -> userService.updateStatus(USER_ID.toString(), req))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("code", "AUTH_031");
  }
}
