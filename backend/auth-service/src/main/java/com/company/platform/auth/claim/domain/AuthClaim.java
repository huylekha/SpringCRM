package com.company.platform.auth.claim.domain;

import com.company.platform.shared.entity.FullAuditEntityUUID;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "auth_claim")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthClaim extends FullAuditEntityUUID {

  @Column(name = "claim_code", nullable = false, unique = true, length = 120)
  private String claimCode;

  @Column(name = "claim_name", nullable = false, length = 150)
  private String claimName;
}
