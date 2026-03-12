package com.company.platform.auth.role.service;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.auth.claim.dto.response.ClaimResponse;
import com.company.platform.auth.claim.repository.AuthClaimRepository;
import com.company.platform.auth.permission.domain.AuthPermission;
import com.company.platform.auth.permission.dto.response.PermissionResponse;
import com.company.platform.auth.permission.repository.AuthPermissionRepository;
import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.auth.role.dto.request.*;
import com.company.platform.auth.role.dto.response.*;
import com.company.platform.auth.role.repository.AuthRoleRepository;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.exception.ErrorCode;
import com.company.platform.shared.exception.ResourceNotFoundException;
import com.company.platform.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final AuthRoleRepository roleRepository;
    private final AuthClaimRepository claimRepository;
    private final AuthPermissionRepository permissionRepository;
    private final PermissionEvaluator permissionEvaluator;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsByRoleCodeAndDeletedFalse(request.getRoleCode())) {
            throw new DuplicateResourceException(ErrorCode.AUTH_ROLE_ALREADY_EXISTS);
        }
        AuthRole role = AuthRole.builder()
                .roleCode(request.getRoleCode().toUpperCase())
                .roleName(request.getRoleName())
                .description(request.getDescription())
                .createdBy(permissionEvaluator.currentUserId())
                .build();
        role = roleRepository.save(role);
        return toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public RoleDetailResponse getRole(String id) {
        AuthRole role = findActiveRole(id);
        List<ClaimResponse> claims = role.getClaims().stream()
                .map(c -> ClaimResponse.builder()
                        .id(c.getId()).claimCode(c.getClaimCode()).claimName(c.getClaimName()).build())
                .toList();
        List<PermissionResponse> permissions = role.getPermissions().stream()
                .map(p -> PermissionResponse.builder()
                        .id(p.getId()).permissionCode(p.getPermissionCode())
                        .resourceName(p.getResourceName()).actionName(p.getActionName()).build())
                .toList();
        return RoleDetailResponse.builder()
                .id(role.getId()).roleCode(role.getRoleCode()).roleName(role.getRoleName())
                .description(role.getDescription())
                .claims(claims).permissions(permissions)
                .createdAt(role.getCreatedAt()).updatedAt(role.getUpdatedAt())
                .build();
    }

    @Transactional
    public RoleResponse updateRole(String id, UpdateRoleRequest request) {
        AuthRole role = findActiveRole(id);
        if (request.getRoleName() != null) role.setRoleName(request.getRoleName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());
        role.setUpdatedBy(permissionEvaluator.currentUserId());
        role = roleRepository.save(role);
        return toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public PageResponse<RoleResponse> listRoles(Pageable pageable) {
        Page<AuthRole> page = roleRepository.findAllByDeletedFalse(pageable);
        List<RoleResponse> content = page.getContent().stream()
                .map(this::toRoleResponse).toList();
        return PageResponse.<RoleResponse>builder()
                .content(content).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    @Transactional
    public void assignClaims(String roleId, AssignClaimsRequest request) {
        AuthRole role = findActiveRole(roleId);
        List<AuthClaim> claims = request.getClaimIds().stream()
                .map(cid -> claimRepository.findByIdAndDeletedFalse(cid)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AUTH_CLAIM_NOT_FOUND)))
                .toList();
        role.getClaims().addAll(claims);
        role.setUpdatedBy(permissionEvaluator.currentUserId());
        roleRepository.save(role);
    }

    @Transactional
    public void removeClaim(String roleId, String claimId) {
        AuthRole role = findActiveRole(roleId);
        role.getClaims().removeIf(c -> c.getId().equals(claimId));
        role.setUpdatedBy(permissionEvaluator.currentUserId());
        roleRepository.save(role);
    }

    @Transactional
    public void assignPermissions(String roleId, AssignPermissionsRequest request) {
        AuthRole role = findActiveRole(roleId);
        List<AuthPermission> perms = request.getPermissionIds().stream()
                .map(pid -> permissionRepository.findByIdAndDeletedFalse(pid)
                        .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AUTH_PERMISSION_NOT_FOUND)))
                .toList();
        role.getPermissions().addAll(perms);
        role.setUpdatedBy(permissionEvaluator.currentUserId());
        roleRepository.save(role);
    }

    @Transactional
    public void removePermission(String roleId, String permissionId) {
        AuthRole role = findActiveRole(roleId);
        role.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        role.setUpdatedBy(permissionEvaluator.currentUserId());
        roleRepository.save(role);
    }

    private AuthRole findActiveRole(String id) {
        return roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.AUTH_ROLE_NOT_FOUND));
    }

    private RoleResponse toRoleResponse(AuthRole r) {
        return RoleResponse.builder()
                .id(r.getId()).roleCode(r.getRoleCode()).roleName(r.getRoleName())
                .description(r.getDescription())
                .createdAt(r.getCreatedAt()).createdBy(r.getCreatedBy())
                .build();
    }
}
