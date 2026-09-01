package com.getit.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.dashboard.dto.DashboardSummaryResult;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class DashboardSummaryServiceTest {

  @Autowired
  private DashboardSummaryService dashboardSummaryService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Test
  @DisplayName("활성 기수 기준 지원자 수 · 미평가 과제 수와, 기수 무관 부원 수 · 미확인 질문 수를 합쳐 반환한다")
  void returnsCombinedSummary() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    Long generationId = generationRepository.save(generation).getId();

    Application submitted = applicationRepository.save(Application.createDraft(
        1L, generationId, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000"));
    submitted.submit(LocalDateTime.now(), LocalDateTime.now());
    applicationRepository.save(Application.createDraft(
        2L, generationId, "임시저장", "draft@gmail.com", "010-1234-5678", null, null, 2, "2021110001"));

    User member = User.createGuest("google-sub-90", "member@getit.com", "김부원", null);
    member.promoteToMember(9);
    userRepository.save(member);

    questionRepository.save(Question.create(1L, null, "질문"));

    DashboardSummaryResult result = dashboardSummaryService.getSummary();

    assertThat(result.totalApplicants()).isEqualTo(1);
    assertThat(result.memberCount()).isEqualTo(1);
    assertThat(result.unEvaluatedAssignmentCount()).isZero();
    assertThat(result.unansweredQuestionCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("활성 기수가 없으면 totalApplicants · unEvaluatedAssignmentCount 는 0이다")
  void returnsZeroForGenerationScopedCountsWhenNoActiveGeneration() {
    DashboardSummaryResult result = dashboardSummaryService.getSummary();

    assertThat(result.totalApplicants()).isZero();
    assertThat(result.unEvaluatedAssignmentCount()).isZero();
  }
}
