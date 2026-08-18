package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicationAnswerResult;
import com.getit.domain.recruitment.dto.ApplicationFormQuestion;
import com.getit.domain.recruitment.dto.ApplicationFormResult;
import com.getit.domain.recruitment.dto.BasicInfo;
import com.getit.domain.recruitment.dto.MyApplicationResult;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
