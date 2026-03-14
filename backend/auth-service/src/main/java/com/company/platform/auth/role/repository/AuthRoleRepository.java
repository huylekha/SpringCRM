package com.company.platform.auth.role.repository;

import com.company.platform.auth.role.domain.AuthRole;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthRoleRepository extends JpaRepository<AuthRole, String> {
  Optional<AuthRole> findByIdAndDeletedFalse(String id);

  Optional<AuthRole> findByRoleCodeAndDeletedFalse(String roleCode);

  boolean existsByRoleCodeAndDeletedFalse(String roleCode);

  Page<AuthRole> findAllByDeletedFalse(Pageable pageable);
}
