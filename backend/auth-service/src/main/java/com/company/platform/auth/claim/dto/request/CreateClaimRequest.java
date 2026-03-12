package com.company.platform.auth.claim.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CreateClaimRequest {
    @NotBlank @Size(max = 120)
    private String claimCode;

    @NotBlank @Size(max = 150)
    private String claimName;
}
