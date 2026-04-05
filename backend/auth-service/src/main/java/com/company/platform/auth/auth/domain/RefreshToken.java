package com.company.platform.auth.auth.domain;

import com.company.platform.shared.entity.BaseEntityUUID;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "auth_refresh_token")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken extends BaseEntityUUID {

  @Column(name = "token_hash", nullable = false, unique = true, length = 255)
  private String tokenHash;

  @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
  private UUID userId;

  @Column(nullable = false)
  @Builder.Default
  private boolean revoked = false;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;
}
