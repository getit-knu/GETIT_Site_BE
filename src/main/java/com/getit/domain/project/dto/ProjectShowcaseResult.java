package com.getit.domain.project.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ProjectShowcaseResult(
    List<String> semesters,
    List<Item> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {

  public static ProjectShowcaseResult of(List<String> semesters, Page<Item> page) {
    return new ProjectShowcaseResult(
        semesters,
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }

  public record Item(
      Long id,
      String title,
      String teamName,
      String semester,
      String semesterLabel,
      String description,
      List<String> techStacks,
      String codeUrl,
      String demoUrl,
      String thumbnailUrl
  ) { }
}
