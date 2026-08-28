package com.getit.domain.lecture.dto;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

/** 부원 강의 조회 응답. (API 명세서 4.1 ~ 4.3) */
public class LectureResult {

  public record ListResult(
      List<Tab> tabs,
      List<Content> content,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last
  ) {

    public static ListResult of(List<Tab> tabs, Page<Content> content) {
      return new ListResult(
          tabs, content.getContent(), content.getNumber(), content.getSize(),
          content.getTotalElements(), content.getTotalPages(), content.isFirst(), content.isLast());
    }
  }

  public record Tab(Long subCategoryId, String name, long count) { }

  public record Content(
      Long id,
      Integer week,
      String title,
      String subCategoryName,
      String trackName,
      Integer durationMinutes,
      OffsetDateTime deadline,
      boolean completed
  ) { }
}
