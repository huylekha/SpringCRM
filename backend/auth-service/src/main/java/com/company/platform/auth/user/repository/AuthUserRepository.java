package com.company.platform.auth.user.repository;

import com.company.platform.auth.user.domain.AuthUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, String> {
    Optional<AuthUser> findByUsernameAndDeletedFalse(String username);
    Optional<AuthUser> findByEmailAndDeletedFalse(String email);
    Optional<AuthUser> findByIdAndDeletedFalse(String id);
    boolean existsByUsernameAndDeletedFalse(String username);
    boolean existsByEmailAndDeletedFalse(String email);
    Page<AuthUser> findAllByDeletedFalse(Pageable pageable);
    Page<AuthUser> findAllByStatusAndDeletedFalse(String status, Pageable pageable);
    long countByRoles_RoleCodeAndDeletedFalse(String roleCode);
}
