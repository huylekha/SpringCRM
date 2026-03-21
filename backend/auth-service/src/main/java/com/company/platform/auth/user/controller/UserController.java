package com.company.platform.auth.user.controller;

import com.company.platform.auth.user.application.command.CreateUserCommand;
import com.company.platform.auth.user.application.command.CreateUserResponse;
import com.company.platform.auth.user.dto.request.*;
import com.company.platform.auth.user.dto.response.*;
import com.company.platform.auth.user.service.UserService;
import com.company.platform.shared.cqrs.CommandBus;
import com.company.platform.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final CommandBus commandBus;

  @PostMapping
  @PreAuthorize("@perm.has('user:create')")
  public ResponseEntity<CreateUserResponse> createUser(
      @Valid @RequestBody CreateUserRequest request) {
    CreateUserCommand command =
        CreateUserCommand.builder()
            .username(request.getUsername())
            .email(request.getEmail())
            .password(request.getPassword())
            .fullName(request.getFullName())
            .status(request.getStatus())
            .build();

    CreateUserResponse response = commandBus.send(command);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{id}")
  @PreAuthorize("@perm.has('user:read')")
  public ResponseEntity<UserDetailResponse> getUser(@PathVariable String id) {
    return ResponseEntity.ok(userService.getUser(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@perm.has('user:update')")
  public ResponseEntity<UserResponse> updateUser(
      @PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
    return ResponseEntity.ok(userService.updateUser(id, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("@perm.has('user:update')")
  public ResponseEntity<UserResponse> updateStatus(
      @PathVariable String id, @Valid @RequestBody StatusUpdateRequest request) {
    return ResponseEntity.ok(userService.updateStatus(id, request));
  }

  @GetMapping
  @PreAuthorize("@perm.has('user:read')")
  public ResponseEntity<PageResponse<UserResponse>> listUsers(
      @RequestParam(required = false) String status,
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(userService.listUsers(status, pageable));
  }

  @PostMapping("/{id}/roles")
  @PreAuthorize("@perm.has('user:assign_role')")
  public ResponseEntity<AssignRolesResponse> assignRoles(
      @PathVariable String id, @Valid @RequestBody AssignRolesRequest request) {
    return ResponseEntity.ok(userService.assignRoles(id, request));
  }

  @DeleteMapping("/{id}/roles/{roleId}")
  @PreAuthorize("@perm.has('user:assign_role')")
  public ResponseEntity<Void> removeRole(@PathVariable String id, @PathVariable String roleId) {
    userService.removeRole(id, roleId);
    return ResponseEntity.noContent().build();
  }
}
