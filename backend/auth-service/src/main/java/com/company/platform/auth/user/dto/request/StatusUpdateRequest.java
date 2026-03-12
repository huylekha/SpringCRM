package com.company.platform.auth.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class StatusUpdateRequest {
    @NotBlank
    private String status;
}
