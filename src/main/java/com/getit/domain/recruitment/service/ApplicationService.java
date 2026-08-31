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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
   * 모집 기간 경계를 판단하는 시각 소스. (PR #179 리뷰 지적)
   *
   * <p>{@code LocalDateTime.now()} 를 직접 부르면 "시작 직전 · 마감 직후" 같은 경계를 테스트에서
   * 고정할 수 없다. 하필 그 경계가 이 서비스의 판단 기준이다.
   * {@code RecruitmentStatusService} 와 같은 {@code Clock} 을 쓴다.
   */
  private final Clock clock;

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
        schedule.resolvePhase(LocalDateTime.now(clock)),
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
   *
   * <p>다만 서류 접수 기간인지는 확인한다 (PR #46 리뷰 지적 — 마감이 지나도 임시 저장이 계속
   * 가능했던 문제). 활성 기수가 있어도 일정이 없거나 접수 기간이 아니면 저장할 수 없다.
   */
  @Transactional
  public DraftSaveResult saveDraft(Long userId, ApplicationDraftRequest request) {
    GenerationSummary activeGeneration = findActiveGeneration();
    findOpenSchedule(activeGeneration.id());

    Application application = upsertApplication(userId, activeGeneration.id(), request.basicInfo());
    upsertAnswers(application.getId(), request.answers());

    // application.getUpdatedAt() 은 이 트랜잭션이 커밋(flush)되기 전이라 아직 갱신되지 않았을 수
    // 있다. "지금 저장한 시각"은 굳이 엔티티를 거치지 않고 바로 써도 의미가 같다.
    LocalDateTime savedAt = LocalDateTime.now(clock);
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
    findOpenSchedule(activeGeneration.id());
    LocalDateTime now = LocalDateTime.now(clock);

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

    ApplicationSubmissionValidator.validateBasicInfo(application);

    List<ApplicationQuestion> questions =
        applicationQuestionRepository.findByGenerationId(activeGeneration.id());
    List<ApplicationAnswer> answers = applicationAnswerRepository.findByApplicationId(application.getId());
    ApplicationSubmissionValidator.validateRequiredAnswers(questions, answers);
    ApplicationSubmissionValidator.validateAnswerLengths(questions, answers);

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
        application.getStatus().label(),
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
              basicInfo.studentNumber());
          return existing;
        })
        .orElseGet(() -> applicationRepository.save(Application.createDraft(
            userId, generationId, basicInfo.name(), basicInfo.email(), basicInfo.phoneNumber(),
            basicInfo.collegeId(), basicInfo.majorId(), basicInfo.grade(), basicInfo.studentNumber())));
  }

  private void assertDraft(Application application) {
    if (application.getStatus() != ApplicationStatus.DRAFT) {
      throw new BusinessException(RecruitmentErrorCode.ALREADY_SUBMITTED);
    }
  }

  /**
   * 답변 upsert. 요청에 없는 질문의 기존 답변은 지우지 않는다 (이슈 #44 논의 필요 사항 참고).
   *
   * <p>PR #46 리뷰 지적으로 개선 — 답변마다 존재 여부를 조회하면 질문 수만큼 SELECT 가 나갔다
   * ({@code findByApplicationIdAndQuestionId} 를 answers.size() 번 호출). 지원서에 달린 답변을
   * 한 번만 조회해 questionId 로 묶어두고 그 맵으로 upsert 여부를 판단한다.
   */
  private void upsertAnswers(Long applicationId, List<ApplicationAnswerRequest> answers) {
    if (answers == null) {
      return;
    }
    Map<Long, ApplicationAnswer> existingByQuestionId =
        applicationAnswerRepository.findByApplicationId(applicationId).stream()
            .collect(Collectors.toMap(ApplicationAnswer::getQuestionId, Function.identity()));

    for (ApplicationAnswerRequest answer : answers) {
      ApplicationAnswer existing = existingByQuestionId.get(answer.questionId());
      if (existing != null) {
        existing.update(answer.answerText(), answer.selectedOptions());
      } else {
        applicationAnswerRepository.save(ApplicationAnswer.create(
            applicationId, answer.questionId(), answer.answerText(), answer.selectedOptions()));
      }
    }
  }

  // 한글 라벨 매핑은 ApplicationStatus.label() 로 옮겼다 (7.6 엑셀 다운로드와 공유, PR #54 리뷰 지적).

  /** 로그인 사용자의 User 정보로 채운다. 값이 없으면 null (명세서 3.1). */
  private BasicInfo resolvePrefill(Long userId) {
    return userAccountService.findActiveById(userId)
        .map(this::toBasicInfo)
        .orElseGet(() -> new BasicInfo(null, null, null, null, null, null, null));
  }

  /**
   * 3.1 프리필. {@code User} 에 있는 값으로 지원서 폼의 첫 화면을 채운다.
   *
   * <p>{@code collegeId} · {@code majorId} 는 여기서 채울 수 없어 항상 {@code null} 이다 —
   * {@code User} 는 단과대 · 학과를 <b>이름</b>으로 들고 있어서(승격 때 이름으로 복사된다)
   * 되돌릴 id 가 없다. 이 값은 지원자가 폼에서 고르고 저장(3.2) 경로에서 담긴다.
   *
   * <p>{@code grade} 는 {@code User.studentYear} 에 대응한다.
   */
  private BasicInfo toBasicInfo(UserAccount account) {
    return new BasicInfo(
        account.name(), account.email(), account.phoneNumber(),
        null, null, account.studentYear(), account.studentNumber());
  }

  private RecruitmentSchedule findSchedule(Long generationId) {
    return recruitmentScheduleRepository.findByGenerationId(generationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.SCHEDULE_NOT_FOUND));
  }

  /**
   * 서류 접수 기간(3.3 · 3.4 공용) 검증. 일정 자체가 없거나 접수 기간이 아니면 예외를 던진다.
   * {@code submit} 은 반환된 일정을 쓰지 않지만(진행 중인 기간이라는 사실만 필요), {@code saveDraft}
   * 와 시그니처를 맞추기 위해 그대로 {@code RecruitmentSchedule} 을 반환한다.
   */
  private RecruitmentSchedule findOpenSchedule(Long generationId) {
    RecruitmentSchedule schedule = recruitmentScheduleRepository.findByGenerationId(generationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OPEN));

    // 시작 전과 마감 후를 나눈다. 한 오류로 묶으면 모집 시작 직전에 들어온 지원자가
    // "제출 기한이 지났습니다" 를 보게 된다 (이슈 #175).
    //
    // 시작 전은 일정이 아직 없을 때와 같은 APPLICATION_NOT_OPEN 을 쓴다. 지원자에게는 둘 다
    // "아직 모집을 받고 있지 않다" 는 한 가지 사실이고, 프론트가 코드로 분기하므로 같은
    // 상황에 코드를 새로 늘리지 않는다 (PR #179 리뷰 지적).
    LocalDateTime now = LocalDateTime.now(clock);
    if (now.isBefore(schedule.getDocumentStartAt())) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_OPEN);
    }
    if (now.isAfter(schedule.getDocumentEndAt())) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_DEADLINE_PASSED);
    }
    // 공개 화면의 표시만 바꾸고 여기를 열어 두면 스위치가 의미가 없다 (이슈 #170).
    if (!schedule.acceptsApplicationAt(now)) {
      throw new BusinessException(RecruitmentErrorCode.APPLICATION_PAUSED);
    }
    return schedule;
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
