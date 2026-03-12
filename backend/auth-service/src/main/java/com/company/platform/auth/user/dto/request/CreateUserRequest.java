package com.company.platform.auth.user.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
public class CreateUserRequest {
    @NotBlank @Size(max = 100)
    private String username;

    @NotBlank @Email @Size(max = 255)
    private String email;

    @NotBlank @Size(min = 8)
    private String password;

    @NotBlank @Size(max = 200)
    private String fullName;

    private String status = "ACTIVE";
}
