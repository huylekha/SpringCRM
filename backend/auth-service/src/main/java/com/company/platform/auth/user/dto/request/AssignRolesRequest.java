package com.company.platform.auth.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @NoArgsConstructor
public class AssignRolesRequest {
    @NotEmpty
    private List<String> roleIds;
}
