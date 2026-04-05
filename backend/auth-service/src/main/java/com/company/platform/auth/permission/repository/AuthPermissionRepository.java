package com.company.platform.auth.permission.repository;

import com.company.platform.auth.permission.domain.AuthPermission;
import com.company.platform.shared.repository.UUIDRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthPermissionRepository extends UUIDRepository<AuthPermission> {
  Optional<AuthPermission> findByIdAndDeletedFalse(UUID id);

  boolean existsByPermissionCodeAndDeletedFalse(String permissionCode);

  Page<AuthPermission> findAllByDeletedFalse(Pageable pageable);
}
