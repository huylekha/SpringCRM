package com.company.platform.shared.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SearchRequest {

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private List<SearchFilter> filters = new ArrayList<>();

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private List<SearchSort> sort = new ArrayList<>();

  @Min(0)
  private int page = 0;

  @Min(1)
  @Max(100)
  private int size = 20;

  public List<SearchFilter> getFilters() {
    return Collections.unmodifiableList(filters);
  }

  public List<SearchSort> getSort() {
    return Collections.unmodifiableList(sort);
  }

  public void setFilters(List<SearchFilter> filters) {
    this.filters = filters != null ? new ArrayList<>(filters) : new ArrayList<>();
  }

  public void setSort(List<SearchSort> sort) {
    this.sort = sort != null ? new ArrayList<>(sort) : new ArrayList<>();
  }
}
