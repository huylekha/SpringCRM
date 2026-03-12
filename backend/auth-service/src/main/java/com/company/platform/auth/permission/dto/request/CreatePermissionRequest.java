package com.company.platform.auth.permission.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CreatePermissionRequest {
    @NotBlank @Size(max = 150)
    private String permissionCode;

    @NotBlank @Size(max = 80)
    private String resourceName;

    @NotBlank @Size(max = 80)
    private String actionName;
}
