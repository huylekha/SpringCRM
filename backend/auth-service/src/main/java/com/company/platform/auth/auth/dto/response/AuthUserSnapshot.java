package com.company.platform.auth.auth.dto.response;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthUserSnapshot {
    private String id;
    private String username;
    private String email;
    private List<String> roles;
    private List<String> claims;
    private String status;
}
