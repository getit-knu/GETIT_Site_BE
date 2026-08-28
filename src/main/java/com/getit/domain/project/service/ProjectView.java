package com.getit.domain.project.service;

import com.getit.domain.project.entity.Project;
import java.util.List;

public record ProjectView(
    Long id,
    String title,
    String teamName,
    String semester,
    String description,
    List<String> techStacks,
    String codeUrl,
    String demoUrl,
    Long fileId,
    boolean isFeatured,
    int order
) {

  public static ProjectView from(Project project) {
    return new ProjectView(
        project.getId(),
        project.getTitle(),
        project.getTeamName(),
        project.getSemester(),
        project.getDescription(),
        project.getTechStacks(),
        project.getCodeUrl(),
        project.getDemoUrl(),
        project.getFileId(),
        project.isFeatured(),
        project.getOrder());
  }
}
