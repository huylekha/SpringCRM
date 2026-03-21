package com.company.platform.shared.response;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
public class PageResponse<T> {

  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private List<SortInfo> sort;

  public PageResponse(
      List<T> content,
      int page,
      int size,
      long totalElements,
      int totalPages,
      List<SortInfo> sort) {
    this.content = content != null ? List.copyOf(content) : null;
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.sort = sort != null ? List.copyOf(sort) : null;
  }

  public List<T> getContent() {
    return content != null ? Collections.unmodifiableList(content) : null;
  }

  public List<SortInfo> getSort() {
    return sort != null ? Collections.unmodifiableList(sort) : null;
  }

  @Getter
  @NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class SortInfo {
    private String field;
    private String direction;
  }
}
