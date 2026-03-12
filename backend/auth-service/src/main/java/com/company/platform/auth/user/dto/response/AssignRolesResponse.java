package com.company.platform.auth.user.dto.response;

import com.company.platform.auth.role.dto.response.RoleSummary;
import lombok.*;
import java.time.Instant;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssignRolesResponse {
    private String userId;
    private List<RoleSummary> roles;
    private Instant assignedAt;
    private String assignedBy;
}
