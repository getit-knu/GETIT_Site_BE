package com.getit.domain.project.dto;

import java.util.List;

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
