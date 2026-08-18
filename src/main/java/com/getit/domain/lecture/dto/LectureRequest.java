package com.getit.domain.lecture.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class LectureRequest {

  /** 8.2 강의 추가. {@code generationId} 생략 시 활성 기수로 채운다. */
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

    /** isPublished 는 명세서 기본값이 false 다. */
    public boolean isPublishedOrDefault() {
      return Boolean.TRUE.equals(isPublished);
    }
  }

  public record AssignmentPart(
      @NotBlank String title,
      @NotBlank String description,
      @NotNull LocalDateTime deadline
  ) { }
}
