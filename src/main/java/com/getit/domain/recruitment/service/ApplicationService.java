package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicationAnswerRequest;
import com.getit.domain.recruitment.dto.ApplicationAnswerResult;
import com.getit.domain.recruitment.dto.ApplicationDecisionResult;
import com.getit.domain.recruitment.dto.ApplicationDraftRequest;
import com.getit.domain.recruitment.dto.ApplicationFormQuestion;
import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.BasicInfo;
import com.getit.domain.recruitment.dto.DraftSaveResult;
import com.getit.domain.recruitment.dto.MyApplicationResult;
import com.getit.domain.recruitment.dto.NextStep;
import com.getit.domain.recruitment.dto.SubmitResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** 지원서 양식 조회 · 내 지원서 조회. (API 명세서 3.1 · 3.2) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationService {

  private final ApplicationRepository applicationRepository;
  private final ApplicationAnswerRepository applicationAnswerRepository;
  private final ApplicationQuestionRepository applicationQuestionRepository;
  private final RecruitmentScheduleRepository recruitmentScheduleRepository;
  private final GenerationQueryService generationQueryService;
  private final UserAccountService userAccountService;

  /**
   * 3.1. 활성 기수 · 일정이 없으면 양식 자체를 그릴 수 없으므로 6.1 · 6.3 과 동일하게 예외로 처리한다.
   */
  public ApplicationFormResult getForm(Long userId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    RecruitmentSchedule schedule = findSchedule(activeGeneration.id());

    List<ApplicationFormQuestion> questions =
        applicationQuestionRepository.findByGenerationId(activeGeneration.id()).stream()
            .map(ApplicationFormQuestion::from)
            .toList();

    return new ApplicationFormResult(
        activeGeneration.generationNo(),
        schedule.resolvePhase(LocalDateTime.now()),
        schedule.getDocumentEndAt(),
        resolvePrefill(userId),
        questions
    );
  }

  /**
   * 3.2. 활성 기수가 없거나(모집 CLOSED) 지원서가 없으면 "지원서 없음"과 동일하게 null 을 반환한다.
   * 3.1 과 달리 에러로 취급하지 않는다 — 조회 자체는 실패가 아니기 때문이다.
   */
  public MyApplicationResult getMyApplication(Long userId) {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();
    if (activeGeneration.isEmpty()) {
      return null;
    }

    Long generationId = activeGeneration.get().id();
    return applicationRepository.findByUserIdAndGenerationId(userId, generationId)
        .map(application -> {
          List<ApplicationAnswerResult> answers =
              applicationAnswerRepository.findByApplicationId(application.getId()).stream()
                  .map(ApplicationAnswerResult::from)
                  .toList();
          return MyApplicationResult.of(application, activeGeneration.get().generationNo(), answers);
        })
        .orElse(null);
  }

  /**
   * 3.3. 임시 저장. 지원서가 없으면 새로 만들고(DRAFT), 있으면 덮어쓴다.
   * 필수값 · 글자수 검증은 하지 않는다 — 검증은 제출(3.4) 시점에만 한다.
   */
  @Transactional
  public DraftSaveResult saveDraft(Long userId, ApplicationDraftRequest request) {
    GenerationSummary activeGeneration = findActiveGeneration();

    Application application = upsertApplication(userId, activeGeneration.id(), request.basicInfo());
    upsertAnswers(application.getId(), request.answers());

    // application.getUpdatedAt() 은 이 트랜잭션이 커밋(flush)되기 전이라 아직 갱신되지 않았을 수
    // 있다. "지금 저장한 시각"은 굳이 엔티티를 거치지 않고 바로 써도 의미가 같다.
    LocalDateTime savedAt = LocalDateTime.now();
    return new DraftSaveResult(application.getId(), application.getStatus(), savedAt);
  }

  /**
   * 3.4. 제출. 검증 순서는 명세서 그대로: 활성 기수 → 서류 기간 → 기존 상태 → 기본 정보 →
   * 필수 답변 → 글자수. {@code request} 가 없으면(본문 없이 제출) 이미 저장된 draft 값으로 검증한다.
   */
  @Transactional
  public SubmitResult submit(Long userId, ApplicationDraftRequest request) {
    GenerationSummary activeGeneration = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OPEN));
    RecruitmentSchedule schedule = recruitmentScheduleRepository.findByGenerationId(activeGeneration.id())
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OPEN));

    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(schedule.getDocumentStartAt()) || now.isAfter(schedule.getDocumentEndAt())) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_DEADLINE_PASSED);
    }

    Optional<Application> existing =
        applicationRepository.findByUserIdAndGenerationId(userId, activeGeneration.id());
    existing.ifPresent(this::assertDraft);

    Application application;
    if (request != null) {
      application = upsertApplication(userId, activeGeneration.id(), request.basicInfo());
      upsertAnswers(application.getId(), request.answers());
    } else {
      application = existing.orElseThrow(() -> new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "제출할 지원서가 없습니다. 기본 정보와 답변을 함께 보내주세요."));
    }

    validateBasicInfo(application);

    List<ApplicationQuestion> questions =
        applicationQuestionRepository.findByGenerationId(activeGeneration.id());
    List<ApplicationAnswer> answers = applicationAnswerRepository.findByApplicationId(application.getId());
    validateRequiredAnswers(questions, answers);
    validateAnswerLengths(questions, answers);

    application.submit(now);
    return new SubmitResult(application.getId(), application.getStatus(), now);
  }

  /**
   * 3.5. 결과 조회. 활성 기수에 제출한(= DRAFT 가 아닌) 지원서가 없으면 조회할 결과가 없는 것이므로
   * 404 로 처리한다.
   */
  public ApplicationDecisionResult getResult(Long userId) {
    GenerationSummary activeGeneration = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(
            CommonErrorCode.RESOURCE_NOT_FOUND, "제출한 지원서가 없습니다."));

    Application application = applicationRepository
        .findByUserIdAndGenerationId(userId, activeGeneration.id())
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(
            CommonErrorCode.RESOURCE_NOT_FOUND, "제출한 지원서가 없습니다."));

    RecruitmentSchedule schedule = findSchedule(activeGeneration.id());
    boolean announced = application.getStatus() != ApplicationStatus.SUBMITTED;
    NextStep nextStep = announced && application.getStatus() == ApplicationStatus.DOC_PASS
        ? new NextStep(
            "INTERVIEW",
            "면접 일정은 개별 안내드립니다.",
            schedule.getInterviewStartAt().toLocalDate(),
            schedule.getInterviewEndAt().toLocalDate())
        : null;

    return new ApplicationDecisionResult(
        activeGeneration.generationNo(),
        application.getStatus(),
        statusLabel(application.getStatus()),
        schedule.getDocumentEndAt(),
        schedule.getInterviewEndAt(),
        nextStep
    );
  }

  /** upsert(3.3 · 3.4 공용). 있으면 DRAFT 인지 확인 후 덮어쓰고, 없으면 새로 만든다. */
  private Application upsertApplication(Long userId, Long generationId, BasicInfo basicInfo) {
    return applicationRepository.findByUserIdAndGenerationId(userId, generationId)
        .map(existing -> {
          assertDraft(existing);
          existing.updateDraft(
              basicInfo.name(), basicInfo.email(), basicInfo.phoneNumber(),
              basicInfo.collegeId(), basicInfo.majorId(), basicInfo.grade(),
              existing.getStudentNumber());
          return existing;
        })
        .orElseGet(() -> applicationRepository.save(Application.createDraft(
            userId, generationId, basicInfo.name(), basicInfo.email(), basicInfo.phoneNumber(),
            basicInfo.collegeId(), basicInfo.majorId(), basicInfo.grade(), null)));
  }

  private void assertDraft(Application application) {
    if (application.getStatus() != ApplicationStatus.DRAFT) {
      throw new BusinessException(RecruitmentErrorCode.ALREADY_SUBMITTED);
    }
  }

  /** 답변 upsert. 요청에 없는 질문의 기존 답변은 지우지 않는다 (이슈 #44 논의 필요 사항 참고). */
  private void upsertAnswers(Long applicationId, List<ApplicationAnswerRequest> answers) {
    if (answers == null) {
      return;
    }
    for (ApplicationAnswerRequest answer : answers) {
      applicationAnswerRepository.findByApplicationIdAndQuestionId(applicationId, answer.questionId())
          .ifPresentOrElse(
              existing -> existing.update(answer.answerText(), answer.selectedOptions()),
              () -> applicationAnswerRepository.save(ApplicationAnswer.create(
                  applicationId, answer.questionId(), answer.answerText(), answer.selectedOptions())));
    }
  }

  /** 명세서 3.4 검증 4단계: 기본 정보(이름 · 이메일 · 연락처 · 단과대학 · 전공 · 학년) 필수. */
  private void validateBasicInfo(Application application) {
    boolean missing = !StringUtils.hasText(application.getName())
        || !StringUtils.hasText(application.getEmail())
        || !StringUtils.hasText(application.getPhoneNumber())
        || application.getCollegeId() == null
        || application.getMajorId() == null
        || application.getGrade() == null;
    if (missing) {
      throw new BusinessException(
          CommonErrorCode.VALIDATION_FAILED, "이름 · 이메일 · 연락처 · 단과대학 · 전공 · 학년을 모두 입력해야 합니다.");
    }
  }

  /** 명세서 3.4 검증 5단계: required = true 질문은 전부 응답해야 한다. */
  private void validateRequiredAnswers(List<ApplicationQuestion> questions, List<ApplicationAnswer> answers) {
    Map<Long, ApplicationAnswer> answersByQuestionId = answers.stream()
        .collect(Collectors.toMap(ApplicationAnswer::getQuestionId, Function.identity()));

    for (ApplicationQuestion question : questions) {
      if (!question.isRequired()) {
        continue;
      }
      ApplicationAnswer answer = answersByQuestionId.get(question.getId());
      if (!isAnswered(answer)) {
        throw new BusinessException(RecruitmentErrorCode.REQUIRED_ANSWER_MISSING);
      }
    }
  }

  private boolean isAnswered(ApplicationAnswer answer) {
    if (answer == null) {
      return false;
    }
    return StringUtils.hasText(answer.getAnswerText())
        || (answer.getSelectedOptions() != null && !answer.getSelectedOptions().isEmpty());
  }

  /** 명세서 3.4 검증 6단계: 각 답변은 질문의 maxLength 이내여야 한다. */
  private void validateAnswerLengths(List<ApplicationQuestion> questions, List<ApplicationAnswer> answers) {
    Map<Long, ApplicationQuestion> questionsById = questions.stream()
        .collect(Collectors.toMap(ApplicationQuestion::getId, Function.identity()));

    for (ApplicationAnswer answer : answers) {
      ApplicationQuestion question = questionsById.get(answer.getQuestionId());
      if (question == null || question.getMaxLength() == null || answer.getAnswerText() == null) {
        continue;
      }
      if (answer.getAnswerText().length() > question.getMaxLength()) {
        throw new BusinessException(RecruitmentErrorCode.ANSWER_LENGTH_EXCEEDED);
      }
    }
  }

  private String statusLabel(ApplicationStatus status) {
    return switch (status) {
      case DRAFT -> "임시 저장";
      case SUBMITTED -> "심사 중";
      case DOC_PASS -> "서류 합격";
      case DOC_FAIL -> "서류 불합격";
      case FINAL_PASS -> "최종 합격";
      case FINAL_FAIL -> "최종 불합격";
    };
  }

  /** 로그인 사용자의 User 정보로 채운다. 값이 없으면 null (명세서 3.1). */
  private BasicInfo resolvePrefill(Long userId) {
    return userAccountService.findActiveById(userId)
        .map(this::toBasicInfo)
        .orElseGet(() -> new BasicInfo(null, null, null, null, null, null));
  }

  /**
   * collegeId · majorId 는 College · Major 마스터 데이터(2.6 · 2.7)가 아직 없어 항상 null 이다.
   * grade 는 {@code User.studentYear} 에 대응한다 (이슈 #38 논의 필요 사항 참고).
   */
  private BasicInfo toBasicInfo(UserAccount account) {
    return new BasicInfo(account.name(), account.email(), account.phoneNumber(), null, null, account.studentYear());
  }

  private RecruitmentSchedule findSchedule(Long generationId) {
    return recruitmentScheduleRepository.findByGenerationId(generationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.SCHEDULE_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
