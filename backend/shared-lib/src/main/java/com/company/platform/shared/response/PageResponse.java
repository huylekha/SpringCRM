package com.company.platform.shared.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private List<SortInfo> sort;

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SortInfo {
    private String field;
    private String direction;
  }
}
