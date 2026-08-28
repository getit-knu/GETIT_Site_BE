package com.getit.domain.project.admin.dto;

import com.getit.domain.project.entity.Project;
import com.getit.domain.project.util.ProjectDateTimes;
import java.time.OffsetDateTime;
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
      boolean isFeatured,
      int order,
      OffsetDateTime createdAt
  ) {

    public static Item of(Project project, String thumbnailUrl) {
      return new Item(
          project.getId(),
          project.getTitle(),
          project.getTeamName(),
          project.getSemester(),
          project.getDescription(),
          project.getTechStacks() == null ? List.of() : project.getTechStacks(),
          project.getCodeUrl(),
          project.getDemoUrl(),
          thumbnailUrl,
          project.isFeatured(),
          project.getOrder(),
          ProjectDateTimes.toOffset(project.getCreatedAt()));
    }
  }
}
