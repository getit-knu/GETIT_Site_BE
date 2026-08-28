package com.getit.domain.qna.admin.dto;

import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.user.entity.Role;
import java.time.OffsetDateTime;

public class AdminQuestionResult {

  public record ListRow(
      int no,
      Long id,
      String authorName,
      String major,
      String content,
      OffsetDateTime createdAt,
      QnaStatus status,
      String statusLabel,
      String lectureTitle
  ) { }

  public record Detail(
      Long id,
      Author author,
      OffsetDateTime createdAt,
      String content,
      QnaStatus status,
      LectureBrief lecture,
      AnswerView answer
  ) { }

  public record Author(Long id, String name, String college, String major, Role role) { }

  public record LectureBrief(Long id, String title) { }

  public record AnswerView(
      Long id,
      Long adminId,
      String adminName,
      String content,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) { }
}
