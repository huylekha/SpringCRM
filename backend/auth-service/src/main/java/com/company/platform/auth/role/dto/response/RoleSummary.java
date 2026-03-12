package com.company.platform.auth.role.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleSummary {
    private String id;
    private String roleCode;
    private String roleName;
}
