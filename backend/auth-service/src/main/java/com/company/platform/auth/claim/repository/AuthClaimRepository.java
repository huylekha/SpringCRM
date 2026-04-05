package com.company.platform.auth.claim.repository;

import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.shared.repository.UUIDRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthClaimRepository extends UUIDRepository<AuthClaim> {
  Optional<AuthClaim> findByIdAndDeletedFalse(UUID id);

  boolean existsByClaimCodeAndDeletedFalse(String claimCode);

  Page<AuthClaim> findAllByDeletedFalse(Pageable pageable);
}
