package com.company.platform.shared.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchRequest {

  private List<SearchFilter> filters = new ArrayList<>();
  private List<SearchSort> sort = new ArrayList<>();

  @Min(0)
  private int page = 0;

  @Min(1)
  @Max(100)
  private int size = 20;
}
