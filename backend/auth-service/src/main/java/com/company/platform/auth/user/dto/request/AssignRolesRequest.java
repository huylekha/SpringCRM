package com.company.platform.auth.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AssignRolesRequest {
  @NotEmpty private List<String> roleIds;
}
