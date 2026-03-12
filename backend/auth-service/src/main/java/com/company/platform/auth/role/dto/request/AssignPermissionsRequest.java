package com.company.platform.auth.role.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class AssignPermissionsRequest {
    @NotEmpty
    private List<String> permissionIds;
}
