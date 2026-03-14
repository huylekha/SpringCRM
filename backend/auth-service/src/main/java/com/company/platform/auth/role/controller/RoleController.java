package com.company.platform.auth.role.controller;

import com.company.platform.auth.role.dto.request.*;
import com.company.platform.auth.role.dto.response.*;
import com.company.platform.auth.role.service.RoleService;
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
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

  private final RoleService roleService;

  @PostMapping
  @PreAuthorize("@perm.has('role:create')")
  public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
  }

  @GetMapping("/{id}")
  @PreAuthorize("@perm.has('role:read')")
  public ResponseEntity<RoleDetailResponse> getRole(@PathVariable String id) {
    return ResponseEntity.ok(roleService.getRole(id));
  }

  @PutMapping("/{id}")
  @PreAuthorize("@perm.has('role:update')")
  public ResponseEntity<RoleResponse> updateRole(
      @PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
    return ResponseEntity.ok(roleService.updateRole(id, request));
  }

  @GetMapping
  @PreAuthorize("@perm.has('role:read')")
  public ResponseEntity<PageResponse<RoleResponse>> listRoles(
      @PageableDefault(size = 20) Pageable pageable) {
    return ResponseEntity.ok(roleService.listRoles(pageable));
  }

  @PostMapping("/{id}/claims")
  @PreAuthorize("@perm.has('role:assign_claim')")
  public ResponseEntity<Void> assignClaims(
      @PathVariable String id, @Valid @RequestBody AssignClaimsRequest request) {
    roleService.assignClaims(id, request);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}/claims/{claimId}")
  @PreAuthorize("@perm.has('role:assign_claim')")
  public ResponseEntity<Void> removeClaim(@PathVariable String id, @PathVariable String claimId) {
    roleService.removeClaim(id, claimId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/permissions")
  @PreAuthorize("@perm.has('role:assign_permission')")
  public ResponseEntity<Void> assignPermissions(
      @PathVariable String id, @Valid @RequestBody AssignPermissionsRequest request) {
    roleService.assignPermissions(id, request);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}/permissions/{permissionId}")
  @PreAuthorize("@perm.has('role:assign_permission')")
  public ResponseEntity<Void> removePermission(
      @PathVariable String id, @PathVariable String permissionId) {
    roleService.removePermission(id, permissionId);
    return ResponseEntity.noContent().build();
  }
}
