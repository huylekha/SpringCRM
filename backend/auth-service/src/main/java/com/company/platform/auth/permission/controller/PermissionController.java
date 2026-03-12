package com.company.platform.auth.permission.controller;

import com.company.platform.auth.permission.dto.request.CreatePermissionRequest;
import com.company.platform.auth.permission.dto.response.PermissionResponse;
import com.company.platform.auth.permission.service.PermissionService;
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
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("@perm.has('permission:create')")
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(permissionService.createPermission(request));
    }

    @GetMapping
    @PreAuthorize("@perm.has('permission:read')")
    public ResponseEntity<PageResponse<PermissionResponse>> listPermissions(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(permissionService.listPermissions(pageable));
    }
}
