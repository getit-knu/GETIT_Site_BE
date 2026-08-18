package com.getit.domain.lecture.dto;

import com.getit.domain.lecture.entity.SubmissionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class LectureRequest {

  public record Create(
      Long generationId,
      @NotNull Long trackId,
      Long subCategoryId,
      @NotNull Integer week,
      @NotBlank String title,
      String description,
      String youtubeUrl,
      String materialUrl,
      Integer durationMinutes,
      List<Long> fileIds,
      Boolean isPublished,
      AssignmentPart assignment
  ) {

    public boolean isPublishedOrDefault() { return Boolean.TRUE.equals(isPublished); }
  }

  public record AssignmentPart(
      @NotBlank String title,
      @NotBlank String description,
      @NotNull LocalDateTime deadline,
      @NotEmpty Set<SubmissionType> allowedTypes,
      String linkPlaceholder
  ) { }
}
