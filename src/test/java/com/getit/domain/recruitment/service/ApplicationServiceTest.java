package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.MyApplicationResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ApplicationServiceTest {

  @Autowired
  private ApplicationService applicationService;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private UserRepository userRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private void saveSchedule() {
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 10, 23, 59, 59),
        LocalDateTime.of(2026, 9, 15, 0, 0)));
  }

  private User saveUser() {
    User user = User.createGuest("google-sub-form", "hong@gmail.com", "홍길동", null);
    user.updateApplicantInfo(null, null, null, 3, null);
    return userRepository.save(user);
  }

  @Nested
  @DisplayName("getForm")
  class GetForm {

    @Test
    @DisplayName("기수 · 단계 · 마감일 · 질문 목록을 반환한다")
    void returnsFormWithQuestions() {
      saveSchedule();
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, 300, null));
      User user = saveUser();

      ApplicationFormResult form = applicationService.getForm(user.getId());

      assertThat(form.generationNo()).isEqualTo(9);
      assertThat(form.phase()).isNotNull();
      assertThat(form.deadline()).isEqualTo(LocalDateTime.of(2026, 9, 10, 23, 59, 59));
      assertThat(form.questions()).hasSize(1);
      assertThat(form.questions().get(0).content()).isEqualTo("지원 동기");
      assertThat(form.questions().get(0).placeholder()).isNull();
    }

    @Test
    @DisplayName("로그인 사용자의 정보로 basicInfoPrefill 을 채운다")
    void fillsPrefillFromUser() {
      saveSchedule();
      User user = saveUser();

      ApplicationFormResult form = applicationService.getForm(user.getId());

      assertThat(form.basicInfoPrefill().name()).isEqualTo("홍길동");
      assertThat(form.basicInfoPrefill().email()).isEqualTo("hong@gmail.com");
      assertThat(form.basicInfoPrefill().grade()).isEqualTo(3);
      assertThat(form.basicInfoPrefill().collegeId()).isNull();
      assertThat(form.basicInfoPrefill().majorId()).isNull();
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 basicInfoPrefill 이 전부 null 이다")
    void prefillIsAllNullWhenUserNotFound() {
      saveSchedule();

      ApplicationFormResult form = applicationService.getForm(999L);

      assertThat(form.basicInfoPrefill().name()).isNull();
      assertThat(form.basicInfoPrefill().email()).isNull();
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationService.getForm(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("모집 일정이 없으면 예외가 발생한다")
    void throwsWhenNoSchedule() {
      assertThatThrownBy(() -> applicationService.getForm(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.SCHEDULE_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getMyApplication")
  class GetMyApplication {

    @Test
    @DisplayName("활성 기수가 없으면 null 을 반환한다")
    void returnsNullWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThat(applicationService.getMyApplication(1L)).isNull();
    }

    @Test
    @DisplayName("지원서가 없으면 null 을 반환한다")
    void returnsNullWhenNoApplication() {
      assertThat(applicationService.getMyApplication(1L)).isNull();
    }

    @Test
    @DisplayName("지원서와 답변을 함께 반환한다")
    void returnsApplicationWithAnswers() {
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678",
          null, null, 2, "2021110000"));
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 30L, null, List.of("sw")));

      MyApplicationResult result = applicationService.getMyApplication(1L);

      assertThat(result.id()).isEqualTo(application.getId());
      assertThat(result.generationNo()).isEqualTo(9);
      assertThat(result.status()).isEqualTo(ApplicationStatus.DRAFT);
      assertThat(result.basicInfo().name()).isEqualTo("홍길동");
      assertThat(result.answers()).hasSize(2);
      assertThat(result.submittedAt()).isNull();
      assertThat(result.savedAt()).isNotNull();
    }

    @Test
    @DisplayName("다른 사용자의 지원서는 반환하지 않는다")
    void doesNotReturnOtherUsersApplication() {
      applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", null, null, null, 2, null));

      assertThat(applicationService.getMyApplication(2L)).isNull();
    }
  }
}
