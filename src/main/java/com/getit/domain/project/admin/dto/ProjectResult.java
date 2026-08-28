package com.getit.domain.project.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.getit.domain.project.entity.Project;
import java.util.List;

public class ProjectResult {

  public record Item(
      Long id,
      String title,
      String teamName,
      String semester,
      String description,
      List<String> techStacks,
      String codeUrl,
      String demoUrl,
      String thumbnailUrl,
      @JsonProperty("isFeatured") boolean isFeatured,
      int order
  ) {

    public static Item of(Project project, String thumbnailUrl) {
      return new Item(
          project.getId(),
          project.getTitle(),
          project.getTeamName(),
          project.getSemester(),
          project.getDescription(),
          project.getTechStacks(),
          project.getCodeUrl(),
          project.getDemoUrl(),
          thumbnailUrl,
          project.isFeatured(),
          project.getOrder());
    }
  }
}
