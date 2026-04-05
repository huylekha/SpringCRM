package com.company.platform.auth.role.repository;

import com.company.platform.auth.role.domain.AuthRole;
import com.company.platform.shared.repository.UUIDRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRoleRepository extends UUIDRepository<AuthRole> {
  Optional<AuthRole> findByIdAndDeletedFalse(UUID id);

  Optional<AuthRole> findByRoleCodeAndDeletedFalse(String roleCode);

  boolean existsByRoleCodeAndDeletedFalse(String roleCode);

  Page<AuthRole> findAllByDeletedFalse(Pageable pageable);
}
