package com.getit.domain.project.admin.dto;

import com.getit.domain.project.dto.ProjectCommand;
import jakarta.validation.constraints.Min;
import com.getit.global.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProjectRequest {

  public record Write(
      @NotBlank @Size(max = 100) String title,
      @NotBlank @Size(max = 100) String teamName,
      @NotBlank @Pattern(regexp = "\\d{4}-(SPRING|SUMMER|FALL|WINTER)") String semester,
      @Size(max = 20000) String description,
      @Size(max = 10) List<@Size(max = 40) @Pattern(regexp = "[^,]+") String> techStacks,
      @HttpUrl String codeUrl,
      @HttpUrl String demoUrl,
      @Positive Long fileId,
      @NotNull Boolean isFeatured,
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
