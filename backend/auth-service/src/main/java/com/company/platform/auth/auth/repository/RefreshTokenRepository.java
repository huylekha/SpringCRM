package com.company.platform.auth.auth.repository;

import com.company.platform.auth.auth.domain.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
  Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

  @Modifying
  @Query(
      "UPDATE RefreshToken r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
  void revokeAllByUserId(String userId);
}
