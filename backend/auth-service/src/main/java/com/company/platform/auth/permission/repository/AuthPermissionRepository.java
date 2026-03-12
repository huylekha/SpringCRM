package com.company.platform.auth.permission.repository;

import com.company.platform.auth.permission.domain.AuthPermission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthPermissionRepository extends JpaRepository<AuthPermission, String> {
    Optional<AuthPermission> findByIdAndDeletedFalse(String id);
    boolean existsByPermissionCodeAndDeletedFalse(String permissionCode);
    Page<AuthPermission> findAllByDeletedFalse(Pageable pageable);
}
