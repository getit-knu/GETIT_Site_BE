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

  /**
   * 내 질문 전체 조회의 한 줄. (이슈 #185)
   *
   * <p>{@link ListItem} 과 달리 <b>어느 강의의 질문인지</b>를 싣는다. 강의별 조회는 강의가 이미
   * 정해져 있지만, 이 목록은 강의를 가로지르므로 없으면 구분이 안 된다.
   *
   * <p>{@code authorName} 은 넣지 않는다 — 전부 내 질문이라 같은 값이 반복될 뿐이다.
   *
   * @param lectureId 강의에 매이지 않은 질문이면 {@code null}
   * @param lectureTitle 강의가 지워졌거나 매이지 않았으면 {@code null}
   */
  public record MyListItem(
      Long id,
      Long lectureId,
      String lectureTitle,
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
