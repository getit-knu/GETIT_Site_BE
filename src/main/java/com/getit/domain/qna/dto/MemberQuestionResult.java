package com.getit.domain.qna.dto;

import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.util.QnaDateTimes;
import java.time.OffsetDateTime;
import java.util.List;

public class MemberQuestionResult {

  public record CreateResult(Long id, String content, QnaStatus status, OffsetDateTime createdAt) {

    public static CreateResult from(Question question) {
      return new CreateResult(
          question.getId(), question.getContent(), question.getStatus(),
          QnaDateTimes.toOffset(question.getCreatedAt()));
    }
  }

  public record ListItem(
      Long id,
      String authorName,
      String content,
      OffsetDateTime createdAt,
      QnaStatus status,
      List<AnswerItem> answers
  ) { }

  public record AnswerItem(Long id, String adminName, String content, OffsetDateTime createdAt) {

    public static AnswerItem of(Answer answer, String adminName) {
      return new AnswerItem(
          answer.getId(), adminName, answer.getContent(), QnaDateTimes.toOffset(answer.getCreatedAt()));
    }
  }
}
