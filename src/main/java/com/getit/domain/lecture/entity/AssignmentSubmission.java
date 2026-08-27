package com.getit.domain.lecture.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
  name = "assignment_submission",
  uniqueConstraints = {
    @UniqueConstraint(
        name = "uk_assignment_submission_assignment_id_user_id",
        columnNames = {"assignment_id", "user_id"})
  }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AssignmentSubmission extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(columnDefinition = "TEXT")
  private String comment;

  @Column(name = "link_url", length = 512)
  private String linkUrl;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private SubmissionStatus status;

  @Column(name = "assignment_id", nullable = false)
  private Long assignmentId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "file_id")
  private Long fileId;

  @Column(name = "submitted_at", nullable = false)
  private LocalDateTime submittedAt;

  @Builder(access = AccessLevel.PRIVATE)
  private AssignmentSubmission(
      String comment,
      String linkUrl,
      SubmissionStatus status,
      Long assignmentId,
      Long userId,
      Long fileId,
      LocalDateTime submittedAt
  ) {
    this.comment = comment;
    this.linkUrl = linkUrl;
    this.status = status;
    this.assignmentId = assignmentId;
    this.userId = userId;
    this.fileId = fileId;
    this.submittedAt = submittedAt;
  }

  public static AssignmentSubmission submit(
      Long assignmentId,
      Long userId,
      Long fileId,
      String linkUrl,
      String comment,
      SubmissionStatus status,
      LocalDateTime submittedAt
  ) {
    return AssignmentSubmission.builder()
        .assignmentId(assignmentId)
        .userId(userId)
        .fileId(fileId)
        .linkUrl(linkUrl)
        .comment(comment)
        .status(status)
        .submittedAt(submittedAt)
        .build();
  }

  public void resubmit(
      Long fileId, String linkUrl, String comment, SubmissionStatus status, LocalDateTime submittedAt) {
    this.fileId = fileId;
    this.linkUrl = linkUrl;
    this.comment = comment;
    this.status = status;
    this.submittedAt = submittedAt;
  }

  public boolean isOwnedBy(Long userId) { return this.userId.equals(userId); }
}
