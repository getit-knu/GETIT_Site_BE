package com.getit.domain.project.admin.dto;

import com.getit.domain.project.dto.ProjectCommand;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProjectRequest {

  public record Write(
      @NotBlank @Size(max = 100) String title,
      @NotBlank @Size(max = 100) String teamName,
      @NotBlank @Size(max = 50) String semester,
      String description,
      List<String> techStacks,
      @Size(max = 512) String codeUrl,
      @Size(max = 512) String demoUrl,
      Long fileId,
      boolean isFeatured,
      @Min(1) Integer order
  ) {

    public ProjectCommand toCommand() {
      return new ProjectCommand(
          title, teamName, semester, description,
          techStacks == null ? List.of() : techStacks,
          codeUrl, demoUrl, isFeatured, fileId);
    }
  }
}
