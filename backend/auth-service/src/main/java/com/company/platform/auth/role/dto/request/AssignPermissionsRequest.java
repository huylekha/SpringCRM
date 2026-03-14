package com.company.platform.auth.role.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignPermissionsRequest {
  @NotEmpty private List<String> permissionIds;
}
