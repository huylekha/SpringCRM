package com.company.platform.auth.role.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateRoleRequest {
  @NotBlank
  @Size(max = 80)
  private String roleCode;

  @NotBlank
  @Size(max = 120)
  private String roleName;

  @Size(max = 300)
  private String description;
}
