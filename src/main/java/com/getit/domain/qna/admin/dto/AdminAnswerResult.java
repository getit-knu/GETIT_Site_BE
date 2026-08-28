package com.getit.domain.qna.admin.dto;

import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.util.QnaDateTimes;
import java.time.OffsetDateTime;

public class AdminAnswerResult {

  public record CreateResult(
      Long id,
      Long questionId,
      String adminName,
      String content,
      OffsetDateTime createdAt,
      QnaStatus questionStatus
  ) {

    public static CreateResult of(Answer answer, String adminName) {
      return new CreateResult(
          answer.getId(), answer.getQuestionId(), adminName, answer.getContent(),
          QnaDateTimes.toOffset(answer.getCreatedAt()), QnaStatus.ANSWERED);
    }
  }

  public record UpdateResult(Long id, String content, OffsetDateTime updatedAt) {

    public static UpdateResult from(Answer answer) {
      return new UpdateResult(
          answer.getId(), answer.getContent(), QnaDateTimes.toOffset(answer.getUpdatedAt()));
    }
  }
}
