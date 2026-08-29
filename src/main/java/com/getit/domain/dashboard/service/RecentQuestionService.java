package com.getit.domain.dashboard.service;

import com.getit.domain.dashboard.dto.RecentQuestionResult;
import com.getit.domain.lecture.service.LectureQueryService;
import com.getit.domain.qna.service.QuestionStatService;
import com.getit.domain.qna.service.RecentQuestion;
import com.getit.domain.user.service.UserQueryService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미확인 Q&A. (API 명세서 5.2)
 *
 * <p>{@code elapsedLabel}은 {@link Clock}을 주입받아 계산한다 — {@code LocalDateTime.now()}를
 * 직접 부르면 테스트에서 실행 시각에 따라 결과가 달라진다({@code RecruitmentStatusService}와
 * 동일한 이유). 새 {@code Clock} 빈을 따로 만들지 않고 이미 있는 빈(recruitment 소유,
 * {@code Asia/Seoul} 고정)을 그대로 재사용한다 — 빈을 두 개 두면 이 타입을 무한정으로 주입받는
 * 기존 지점(예: {@code RecruitmentStatusService})의 주입이 모호해져 애플리케이션 기동이 깨진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentQuestionService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final QuestionStatService questionStatService;
  private final UserQueryService userQueryService;
  private final LectureQueryService lectureQueryService;
  private final Clock clock;

  public List<RecentQuestionResult> getRecentQuestions(int size) {
    List<RecentQuestion> questions = questionStatService.findRecent(size);
    if (questions.isEmpty()) {
      return List.of();
    }

    Map<Long, String> authorNames = userQueryService.findNamesByIds(
        questions.stream().map(RecentQuestion::authorId).toList());
    Map<Long, String> lectureTitles = lectureQueryService.findTitlesByIds(
        questions.stream().map(RecentQuestion::lectureId).filter(Objects::nonNull).distinct().toList());

    LocalDateTime now = LocalDateTime.now(clock);
    return questions.stream()
        .map(question -> toResult(question, authorNames, lectureTitles, now))
        .toList();
  }

  private RecentQuestionResult toResult(
      RecentQuestion question, Map<Long, String> authorNames, Map<Long, String> lectureTitles, LocalDateTime now
  ) {
    return new RecentQuestionResult(
        question.questionId(),
        authorNames.get(question.authorId()),
        question.content(),
        question.createdAt().atZone(SEOUL).toOffsetDateTime(),
        toElapsedLabel(question.createdAt(), now),
        question.lectureId() == null ? null : lectureTitles.get(question.lectureId()));
  }

  /** {@code n분 전} / {@code n시간 전} / {@code n일 전}. */
  private String toElapsedLabel(LocalDateTime createdAt, LocalDateTime now) {
    long minutes = ChronoUnit.MINUTES.between(createdAt, now);
    if (minutes < 1) {
      return "방금 전";
    }
    if (minutes < 60) {
      return minutes + "분 전";
    }
    long hours = minutes / 60;
    if (hours < 24) {
      return hours + "시간 전";
    }
    return (hours / 24) + "일 전";
  }
}
