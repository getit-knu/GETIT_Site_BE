package com.getit.domain.lecture.dto;

import com.getit.domain.lecture.entity.SubmissionStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;

public class LectureResult {

  public record ListResult(
      List<Tab> tabs,
      List<Content> content,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean first,
      boolean last
  ) {

    public static ListResult of(List<Tab> tabs, Page<Content> content) {
      return new ListResult(
          tabs, content.getContent(), content.getNumber(), content.getSize(),
          content.getTotalElements(), content.getTotalPages(), content.isFirst(), content.isLast());
    }
  }

  public record Tab(Long subCategoryId, String name, long count) { }

  public record Content(
      Long id,
      Integer week,
      String title,
      String subCategoryName,
      String trackName,
      Integer durationMinutes,
      OffsetDateTime deadline,
      boolean completed
  ) { }

  public record DetailResult(
      Long id,
      Integer week,
      String title,
      String description,
      String trackName,
      String subCategoryName,
      Integer durationMinutes,
      String youtubeUrl,
      String materialUrl,
      Author author,
      OffsetDateTime publishedAt,
      List<Material> materials,
      AssignmentInfo assignment,
      MySubmission mySubmission
  ) { }

  public record Author(String name, String profileImageUrl) { }

  public record Material(Long fileId, String displayName, Long size, String contentType) { }

  public record AssignmentInfo(Long id, String title, String description, OffsetDateTime deadline) { }

  public record MySubmission(
      Long id,
      String fileUrl,
      String fileName,
      String linkUrl,
      String comment,
      OffsetDateTime submittedAt,
      SubmissionStatus status,
      List<FeedbackItem> feedbacks
  ) { }

  public record FeedbackItem(Long id, String adminName, String content, OffsetDateTime createdAt) { }

  public record DownloadUrl(String downloadUrl, String fileName, String contentType, int expiresIn) { }
}
