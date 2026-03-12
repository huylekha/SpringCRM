package com.company.platform.auth.claim.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClaimResponse {
    private String id;
    private String claimCode;
    private String claimName;
}
