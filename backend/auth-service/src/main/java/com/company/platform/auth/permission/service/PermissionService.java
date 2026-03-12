package com.company.platform.auth.permission.service;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.permission.domain.AuthPermission;
import com.company.platform.auth.permission.dto.request.CreatePermissionRequest;
import com.company.platform.auth.permission.dto.response.PermissionResponse;
import com.company.platform.auth.permission.repository.AuthPermissionRepository;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.exception.ErrorCode;
import com.company.platform.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final AuthPermissionRepository permissionRepository;
    private final PermissionEvaluator permissionEvaluator;

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByPermissionCodeAndDeletedFalse(request.getPermissionCode())) {
            throw new DuplicateResourceException(ErrorCode.AUTH_PERMISSION_ALREADY_EXISTS);
        }
        AuthPermission perm = AuthPermission.builder()
                .permissionCode(request.getPermissionCode())
                .resourceName(request.getResourceName())
                .actionName(request.getActionName())
                .createdBy(permissionEvaluator.currentUserId())
                .build();
        perm = permissionRepository.save(perm);
        return toResponse(perm);
    }

    @Transactional(readOnly = true)
    public PageResponse<PermissionResponse> listPermissions(Pageable pageable) {
        Page<AuthPermission> page = permissionRepository.findAllByDeletedFalse(pageable);
        List<PermissionResponse> content = page.getContent().stream()
                .map(this::toResponse).toList();
        return PageResponse.<PermissionResponse>builder()
                .content(content).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    private PermissionResponse toResponse(AuthPermission p) {
        return PermissionResponse.builder()
                .id(p.getId()).permissionCode(p.getPermissionCode())
                .resourceName(p.getResourceName()).actionName(p.getActionName())
                .build();
    }
}
