package com.getit.domain.lecture.admin.dto;

import com.getit.domain.lecture.entity.SubmissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class LectureRequest {

  public record Create(
      Long generationId,
      @NotNull Long trackId,
      Long subCategoryId,
      @NotNull @Min(1) Integer week,
      @NotBlank @Size(max = 255) String title,
      String description,
      @Size(max = 512) String youtubeUrl,
      @Size(max = 512) String materialUrl,
      @Min(1) Integer durationMinutes,
      List<Long> fileIds,
      Boolean isPublished,
      @Valid AssignmentPart assignment
  ) {

    public boolean isPublishedOrDefault() { return Boolean.TRUE.equals(isPublished); }
  }

  public record Update(
      Long generationId,
      @NotNull Long trackId,
      Long subCategoryId,
      @NotNull @Min(1) Integer week,
      @NotBlank @Size(max = 255) String title,
      String description,
      @Size(max = 512) String youtubeUrl,
      @Size(max = 512) String materialUrl,
      @Min(1) Integer durationMinutes,
      List<Long> fileIds,
      Boolean isPublished,
      @Valid AssignmentPart assignment
  ) {

    public boolean isPublishedOrDefault() { return Boolean.TRUE.equals(isPublished); }
  }

  public record AssignmentPart(
      @NotBlank @Size(max = 255) String title,
      @NotBlank String description,
      @NotNull LocalDateTime deadline,
      @NotEmpty Set<SubmissionType> allowedTypes,
      @Size(max = 255) String linkPlaceholder
  ) { }
}
