package com.getit.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.dashboard.dto.RecentQuestionResult;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/** {@code Clock} 을 고정해서 elapsedLabel 을 결정적으로 검증한다({@code RecruitmentStatusServiceTest}와 동일 패턴). */
@SpringBootTest
@Transactional
class RecentQuestionServiceTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
  private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0, 0);

  @TestConfiguration
  static class FixedClockConfig {

    @Bean
    @Primary
    Clock clock() {
      return Clock.fixed(NOW.atZone(SEOUL).toInstant(), SEOUL);
    }
  }

  @Autowired
  private RecentQuestionService recentQuestionService;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private LectureRepository lectureRepository;

  private void createdAt(Question question, LocalDateTime createdAt) {
    ReflectionTestUtils.setField(question, "createdAt", createdAt);
  }

  @Test
  @DisplayName("작성자 이름 · 경과 라벨 · 강의 제목을 채워 반환한다")
  void returnsEnrichedRecentQuestions() {
    User author = userRepository.save(User.createGuest("google-sub-91", "author@getit.com", "김부원", null));
    Lecture lecture = lectureRepository.save(Lecture.create(
        1, "HTML/CSS 기초", null, null, null, null, true, 1L, null, null, 1L));
    Question withLecture = questionRepository.save(
        Question.create(author.getId(), lecture.getId(), "강의 자료 다운로드 문의"));
    createdAt(withLecture, NOW.minusHours(3));
    Question withoutLecture = questionRepository.save(Question.create(author.getId(), null, "과제 제출 기한 문의"));
    createdAt(withoutLecture, NOW.minusHours(1));
    questionRepository.flush();

    List<RecentQuestionResult> results = recentQuestionService.getRecentQuestions(5);

    assertThat(results).extracting(RecentQuestionResult::authorName).containsExactly("김부원", "김부원");
    RecentQuestionResult recent = results.stream()
        .filter(r -> r.content().equals("과제 제출 기한 문의")).findFirst().orElseThrow();
    assertThat(recent.elapsedLabel()).isEqualTo("1시간 전");
    assertThat(recent.lectureTitle()).isNull();

    RecentQuestionResult withLectureResult = results.stream()
        .filter(r -> r.content().equals("강의 자료 다운로드 문의")).findFirst().orElseThrow();
    assertThat(withLectureResult.elapsedLabel()).isEqualTo("3시간 전");
    assertThat(withLectureResult.lectureTitle()).isEqualTo("HTML/CSS 기초");
  }

  @Test
  @DisplayName("PENDING 질문이 없으면 빈 리스트다")
  void returnsEmptyWhenNoQuestions() {
    assertThat(recentQuestionService.getRecentQuestions(5)).isEmpty();
  }
}
