package com.company.platform.auth.claim.repository;

import com.company.platform.auth.claim.domain.AuthClaim;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClaimRepository extends JpaRepository<AuthClaim, String> {
  Optional<AuthClaim> findByIdAndDeletedFalse(String id);

  boolean existsByClaimCodeAndDeletedFalse(String claimCode);

  Page<AuthClaim> findAllByDeletedFalse(Pageable pageable);
}
