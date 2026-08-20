package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.ApplicationAnswerRequest;
import com.getit.domain.recruitment.dto.ApplicationDecisionResult;
import com.getit.domain.recruitment.dto.ApplicationDraftRequest;
import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.BasicInfo;
import com.getit.domain.recruitment.dto.DraftSaveResult;
import com.getit.domain.recruitment.dto.MyApplicationResult;
import com.getit.domain.recruitment.dto.SubmitResult;
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
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
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

  /** now 가 서류 접수 기간 안에 들어오는 일정. 실제 시각과 무관하게 항상 열려 있다. */
  private void saveOpenSchedule() {
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        now.minusDays(10), now.plusDays(20),
        now.minusDays(5), now.plusDays(5),
        now.plusDays(10)));
  }

  /** now 가 서류 접수 마감을 지난 일정. */
  private void saveClosedSchedule() {
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        now.minusDays(30), now.minusDays(1),
        now.minusDays(30), now.minusDays(20),
        now.minusDays(10)));
  }

  private BasicInfo validBasicInfo() {
    return new BasicInfo("홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2);
  }

  private ApplicationQuestion saveRequiredTextQuestion(Integer maxLength) {
    return applicationQuestionRepository.save(ApplicationQuestion.create(
        activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, maxLength, null));
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

  @Nested
  @DisplayName("saveDraft")
  class SaveDraft {

    @Test
    @DisplayName("지원서가 없으면 새로 생성한다")
    void createsNewApplication() {
      ApplicationDraftRequest request = new ApplicationDraftRequest(validBasicInfo(), List.of(
          new ApplicationAnswerRequest(10L, "지원 동기입니다.", null)));

      DraftSaveResult result = applicationService.saveDraft(1L, request);

      assertThat(result.status()).isEqualTo(ApplicationStatus.DRAFT);
      assertThat(result.savedAt()).isNotNull();
      Application saved = applicationRepository.findByUserIdAndGenerationId(1L, activeGeneration.getId())
          .orElseThrow();
      assertThat(saved.getId()).isEqualTo(result.id());
      assertThat(saved.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("이미 있으면 기본 정보를 덮어쓰고 새로 만들지 않는다")
    void overwritesExistingApplication() {
      DraftSaveResult first = applicationService.saveDraft(1L,
          new ApplicationDraftRequest(validBasicInfo(), null));

      BasicInfo updated = new BasicInfo("홍길동", "hong2@gmail.com", "010-9999-8888", 2L, 22L, 3);
      DraftSaveResult second = applicationService.saveDraft(1L, new ApplicationDraftRequest(updated, null));

      assertThat(second.id()).isEqualTo(first.id());
      assertThat(applicationRepository.count()).isEqualTo(1);
      Application saved = applicationRepository.findById(first.id()).orElseThrow();
      assertThat(saved.getEmail()).isEqualTo("hong2@gmail.com");
      assertThat(saved.getCollegeId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("답변은 있으면 갱신하고 없으면 새로 만든다")
    void upsertsAnswers() {
      applicationService.saveDraft(1L, new ApplicationDraftRequest(validBasicInfo(), List.of(
          new ApplicationAnswerRequest(10L, "원래 답변", null))));

      applicationService.saveDraft(1L, new ApplicationDraftRequest(validBasicInfo(), List.of(
          new ApplicationAnswerRequest(10L, "수정된 답변", null),
          new ApplicationAnswerRequest(30L, null, List.of("sw")))));

      Application application = applicationRepository.findByUserIdAndGenerationId(1L, activeGeneration.getId())
          .orElseThrow();
      List<ApplicationAnswer> answers = applicationAnswerRepository.findByApplicationId(application.getId());
      assertThat(answers).hasSize(2);
      assertThat(answers).extracting(ApplicationAnswer::getAnswerText)
          .contains("수정된 답변");
    }

    @Test
    @DisplayName("이미 제출된 지원서면 예외가 발생한다")
    void throwsWhenAlreadySubmitted() {
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, null));
      application.submit(LocalDateTime.now());

      assertThatThrownBy(() -> applicationService.saveDraft(1L,
          new ApplicationDraftRequest(validBasicInfo(), null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ALREADY_SUBMITTED);
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationService.saveDraft(1L,
          new ApplicationDraftRequest(validBasicInfo(), null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("submit")
  class Submit {

    @Test
    @DisplayName("요청 본문으로 제출하면 저장과 동시에 SUBMITTED 로 전환된다")
    void submitsWithRequestBody() {
      saveOpenSchedule();
      saveRequiredTextQuestion(300);

      SubmitResult result = applicationService.submit(1L, new ApplicationDraftRequest(
          validBasicInfo(), List.of(new ApplicationAnswerRequest(
              applicationQuestionRepository.findByGenerationId(activeGeneration.getId()).get(0).getId(),
              "지원 동기입니다.", null))));

      assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
      assertThat(result.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("본문 없이 제출하면 이미 저장된 draft 값으로 검증한다")
    void submitsWithoutRequestBodyUsingSavedDraft() {
      saveOpenSchedule();
      ApplicationQuestion question = saveRequiredTextQuestion(300);
      applicationService.saveDraft(1L, new ApplicationDraftRequest(validBasicInfo(), List.of(
          new ApplicationAnswerRequest(question.getId(), "지원 동기입니다.", null))));

      SubmitResult result = applicationService.submit(1L, null);

      assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
    }

    @Test
    @DisplayName("본문도 없고 저장된 draft 도 없으면 예외가 발생한다")
    void throwsWhenNoRequestAndNoDraft() {
      saveOpenSchedule();

      assertThatThrownBy(() -> applicationService.submit(1L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationService.submit(1L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_OPEN);
    }

    @Test
    @DisplayName("모집 일정이 없으면 예외가 발생한다")
    void throwsWhenNoSchedule() {
      assertThatThrownBy(() -> applicationService.submit(1L, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_OPEN);
    }

    @Test
    @DisplayName("서류 접수 기간이 아니면 예외가 발생한다")
    void throwsWhenDeadlinePassed() {
      saveClosedSchedule();

      assertThatThrownBy(() -> applicationService.submit(1L,
          new ApplicationDraftRequest(validBasicInfo(), null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_DEADLINE_PASSED);
    }

    @Test
    @DisplayName("이미 제출됐으면 예외가 발생한다")
    void throwsWhenAlreadySubmitted() {
      saveOpenSchedule();
      ApplicationQuestion question = saveRequiredTextQuestion(300);
      ApplicationDraftRequest request = new ApplicationDraftRequest(validBasicInfo(), List.of(
          new ApplicationAnswerRequest(question.getId(), "지원 동기입니다.", null)));
      applicationService.submit(1L, request);

      assertThatThrownBy(() -> applicationService.submit(1L, request))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ALREADY_SUBMITTED);
    }

    @Test
    @DisplayName("기본 정보가 빠지면 예외가 발생한다")
    void throwsWhenBasicInfoMissing() {
      saveOpenSchedule();
      saveRequiredTextQuestion(300);
      BasicInfo incomplete = new BasicInfo("홍길동", "hong@gmail.com", null, 1L, 11L, 2);

      assertThatThrownBy(() -> applicationService.submit(1L, new ApplicationDraftRequest(incomplete, null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("필수 질문에 응답하지 않으면 예외가 발생한다")
    void throwsWhenRequiredAnswerMissing() {
      saveOpenSchedule();
      saveRequiredTextQuestion(300);

      assertThatThrownBy(() -> applicationService.submit(1L,
          new ApplicationDraftRequest(validBasicInfo(), null)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.REQUIRED_ANSWER_MISSING);
    }

    @Test
    @DisplayName("답변이 maxLength 를 초과하면 예외가 발생한다")
    void throwsWhenAnswerTooLong() {
      saveOpenSchedule();
      ApplicationQuestion question = saveRequiredTextQuestion(5);

      assertThatThrownBy(() -> applicationService.submit(1L, new ApplicationDraftRequest(
          validBasicInfo(), List.of(new ApplicationAnswerRequest(question.getId(), "123456", null)))))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ANSWER_LENGTH_EXCEEDED);
    }
  }

  @Nested
  @DisplayName("getResult")
  class GetResult {

    private Application submittedApplication() {
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, null));
      application.submit(LocalDateTime.now());
      return application;
    }

    @Test
    @DisplayName("발표 전(SUBMITTED)이면 심사 중이고 nextStep 이 없다")
    void beforeAnnouncementHidesNextStep() {
      saveOpenSchedule();
      submittedApplication();

      ApplicationDecisionResult result = applicationService.getResult(1L);

      assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
      assertThat(result.statusLabel()).isEqualTo("심사 중");
      assertThat(result.nextStep()).isNull();
    }

    @Test
    @DisplayName("서류 합격이면 면접 안내 nextStep 을 채운다")
    void docPassFillsNextStep() {
      saveOpenSchedule();
      Application application = submittedApplication();
      ReflectionTestUtils.setField(application, "status", ApplicationStatus.DOC_PASS);

      ApplicationDecisionResult result = applicationService.getResult(1L);

      assertThat(result.statusLabel()).isEqualTo("서류 합격");
      assertThat(result.nextStep()).isNotNull();
      assertThat(result.nextStep().type()).isEqualTo("INTERVIEW");
    }

    @Test
    @DisplayName("서류 불합격이면 nextStep 이 없다")
    void docFailHasNoNextStep() {
      saveOpenSchedule();
      Application application = submittedApplication();
      ReflectionTestUtils.setField(application, "status", ApplicationStatus.DOC_FAIL);

      ApplicationDecisionResult result = applicationService.getResult(1L);

      assertThat(result.statusLabel()).isEqualTo("서류 불합격");
      assertThat(result.nextStep()).isNull();
    }

    @Test
    @DisplayName("최종 합격이면 최종 합격 라벨을 반환한다")
    void finalPassLabel() {
      saveOpenSchedule();
      Application application = submittedApplication();
      ReflectionTestUtils.setField(application, "status", ApplicationStatus.FINAL_PASS);

      assertThat(applicationService.getResult(1L).statusLabel()).isEqualTo("최종 합격");
    }

    @Test
    @DisplayName("제출한 지원서가 없으면 예외가 발생한다")
    void throwsWhenNoSubmittedApplication() {
      assertThatThrownBy(() -> applicationService.getResult(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 제출한 것으로 취급하지 않는다")
    void draftIsNotTreatedAsSubmitted() {
      applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, null));

      assertThatThrownBy(() -> applicationService.getResult(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("활성 기수가 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationService.getResult(1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
    }
  }
}
