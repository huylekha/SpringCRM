package com.company.platform.auth.user.repository;

import com.company.platform.auth.user.domain.AuthUser;
import com.company.platform.shared.repository.UUIDRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends UUIDRepository<AuthUser> {
  Optional<AuthUser> findByUsernameAndDeletedFalse(String username);

  Optional<AuthUser> findByEmailAndDeletedFalse(String email);

  Optional<AuthUser> findByIdAndDeletedFalse(UUID id);

  boolean existsByUsernameAndDeletedFalse(String username);

  boolean existsByEmailAndDeletedFalse(String email);

  Page<AuthUser> findAllByDeletedFalse(Pageable pageable);

  Page<AuthUser> findAllByStatusAndDeletedFalse(String status, Pageable pageable);

  long countByRoles_RoleCodeAndDeletedFalse(String roleCode);
}
