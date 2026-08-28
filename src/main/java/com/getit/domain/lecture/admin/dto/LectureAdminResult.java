package com.getit.domain.lecture.admin.dto;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.setting.category.dto.CategorySummary;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class LectureAdminResult {

  public record CreateResult(Long id, String title, Integer week, LocalDateTime createdAt) {

    public static CreateResult from(Lecture lecture) {
      return new CreateResult(lecture.getId(), lecture.getTitle(), lecture.getWeek(), lecture.getCreatedAt());
    }
  }

  public record ListResult(List<CategorySummary> tracks, List<LectureCard> lectures) { }

  public record LectureCard(
      Long id,
      Integer week,
      String title,
      String description,
      LocalDateTime deadline,
      long submittedCount,
      long totalCount,
      long feedbackDoneCount,
      boolean isPublished
  ) {

    public static LectureCard of(
        Lecture lecture, Assignment assignment, long submittedCount, long totalCount, long feedbackDoneCount) {
      return new LectureCard(
          lecture.getId(), lecture.getWeek(), lecture.getTitle(), lecture.getDescription(),
          assignment != null ? assignment.getDeadline() : null,
          submittedCount, totalCount, feedbackDoneCount, lecture.isPublished());
    }
  }

  public record DetailResult(
      Long id,
      Long generationId,
      Long trackId,
      Long subCategoryId,
      Integer week,
      String title,
      String description,
      String youtubeUrl,
      String materialUrl,
      Integer durationMinutes,
      boolean isPublished,
      List<FileItem> files,
      AssignmentResult assignment
  ) {

    public static DetailResult of(Lecture lecture, List<FileItem> files, AssignmentResult assignment) {
      return new DetailResult(
          lecture.getId(), lecture.getGenerationId(), lecture.getTrackId(), lecture.getSubCategoryId(),
          lecture.getWeek(), lecture.getTitle(), lecture.getDescription(), lecture.getYoutubeUrl(),
          lecture.getMaterialUrl(), lecture.getDurationMinutes(), lecture.isPublished(), files, assignment);
    }
  }

  public record FileItem(Long fileId, String displayName, String url, Long size) {

    public static FileItem of(String displayName, FileInfo file) {
      return new FileItem(file.fileId(), displayName, file.url(), file.size());
    }
  }

  public record AssignmentResult(
      Long id,
      String title,
      String description,
      LocalDateTime deadline,
      Set<SubmissionType> allowedTypes,
      String linkPlaceholder
  ) {

    public static AssignmentResult from(Assignment assignment) {
      return new AssignmentResult(
          assignment.getId(), assignment.getTitle(), assignment.getDescription(), assignment.getDeadline(),
          assignment.getAllowedTypes(), assignment.getLinkPlaceholder());
    }
  }
}
