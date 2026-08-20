package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.dto.ApplicationAnswerResult;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 지원자 목록 · 상세 조회. (API 명세서 7.1 · 7.2) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationAdminService {

  private final ApplicationRepository applicationRepository;
  private final ApplicationAnswerRepository applicationAnswerRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * 7.1. 목록은 활성 기수 기준으로 조회한다 (6.8 getCriteria 등 다른 관리자 조회와 동일한 방식).
   * status 필터가 없으면 아직 제출하지 않은(DRAFT) 지원서는 제외한다 — 임시 저장만 하고 제출하지
   * 않은 지원자는 심사 대상이 아니기 때문이다.
   */
  public PageResponse<ApplicantSummary> listApplicants(ApplicationStatus status, Pageable pageable) {
    GenerationSummary activeGeneration = findActiveGeneration();

    Page<Application> applications = status != null
        ? applicationRepository.findByGenerationIdAndStatus(activeGeneration.id(), status, pageable)
        : applicationRepository.findByGenerationIdAndStatusNot(
            activeGeneration.id(), ApplicationStatus.DRAFT, pageable);

    return PageResponse.from(applications, ApplicantSummary::from);
  }

  /**
   * 7.2. 상세 조회는 기수 제한 없이 id 로만 찾는다 — 지난 기수 지원자의 상세도 조회할 수 있어야
   * 하기 때문이다 (목록과 달리 "활성 기수만" 제한을 두지 않는다).
   */
  public ApplicantDetailResult getApplicantDetail(Long applicationId) {
    Application application = applicationRepository.findById(applicationId)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));

    List<ApplicationAnswerResult> answers =
        applicationAnswerRepository.findByApplicationId(application.getId()).stream()
            .map(ApplicationAnswerResult::from)
            .toList();

    return ApplicantDetailResult.of(application, answers);
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
