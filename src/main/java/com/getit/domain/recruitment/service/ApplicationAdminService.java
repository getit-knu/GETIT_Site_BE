package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.AdjacentApplicantResult;
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
import com.getit.domain.user.util.ExcelExporter;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 지원자 목록 · 상세 조회 · 순차탐색 · 엑셀 다운로드. (API 명세서 7.1 · 7.2 · 7.5 · 7.6)
 *
 * <p>서류 평가 저장 · 합불 처리(7.3 · 7.4)는 {@link ApplicationEvaluationService} 참고 — 여기
 * 다 있으면 300줄 제한을 넘어서(PR #54 작업 중 313줄) 분리했다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationAdminService {

  /**
   * 목록(7.1) · 순차탐색(7.5) · 엑셀 다운로드(7.6) 이 공유하는 정렬 기준 — 제출일시 내림차순,
   * id 를 보조 정렬로 둔다. 이 셋이 서로 다른 순서를 쓰면 "지금 보고 있던 목록"과 순차탐색이
   * 어긋난다 (PR #54 Copilot 리뷰 지적 — 7.1 이 클라이언트가 보낸 ?sort= 를 그대로 받아써서,
   * 목록을 이름순으로 보다가 순차탐색을 누르면 다른 순서로 넘어가는 문제가 있었다). 그래서 7.1 도
   * 클라이언트가 보낸 sort 를 무시하고 이 상수로 강제한다.
   */
  private static final Sort APPLICANT_ORDER = Sort.by(Sort.Direction.DESC, "submittedAt", "id");

  private static final DateTimeFormatter EXCEL_DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final ApplicationRepository applicationRepository;
  private final ApplicationAnswerRepository applicationAnswerRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * 7.1. {@code generationId} 가 없으면 활성 기수를 대상으로 한다 (PR #48 리뷰 지적 — 예전에는
   * 항상 활성 기수만 조회할 수 있어서 7.2(상세, 기수 제한 없음)와 스코프가 달랐다. 관리자가
   * 지난 기수 지원자 상세에 도달할 경로가 없던 문제라 목록에서도 기수를 지정할 수 있게 했다).
   *
   * <p>status 필터가 없으면 아직 제출하지 않은(DRAFT) 지원서는 제외한다 — 임시 저장만 하고
   * 제출하지 않은 지원자는 심사 대상이 아니기 때문이다.
   *
   * <p>{@code pageable} 의 page · size 만 쓰고 sort 는 {@link #APPLICANT_ORDER} 로 강제한다
   * (위 필드 설명 참고).
   */
  public PageResponse<ApplicantSummary> listApplicants(Long generationId, ApplicationStatus status, Pageable pageable) {
    Long targetGenerationId = resolveGenerationId(generationId);
    Pageable enforcedOrder = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), APPLICANT_ORDER);

    Page<Application> applications = status != null
        ? applicationRepository.findByGenerationIdAndStatus(targetGenerationId, status, enforcedOrder)
        : applicationRepository.findByGenerationIdAndStatusNot(
            targetGenerationId, ApplicationStatus.DRAFT, enforcedOrder);

    return PageResponse.from(applications, ApplicantSummary::from);
  }

  /**
   * 7.2. 상세 조회는 기수 제한 없이 id 로만 찾는다 — 지난 기수 지원자의 상세도 조회할 수 있어야
   * 하기 때문이다 (목록과 달리 "활성 기수만" 제한을 두지 않는다).
   *
   * <p>DRAFT 는 조회 대상에서 제외한다 (PR #48 리뷰 지적 — 지원자가 작성 중 저장만 해도 관리자가
   * 미완성 답변을 그대로 읽을 수 있었다). {@link com.getit.domain.recruitment.service.ApplicationService#getResult}
   * 가 DRAFT 를 "제출한 지원서 없음"으로 취급하는 것과 같은 방식으로, 없는 지원서와 동일하게 404 로
   * 처리한다.
   */
  public ApplicantDetailResult getApplicantDetail(Long applicationId) {
    Application application = findEvaluableApplication(applicationId);

    List<ApplicationAnswerResult> answers =
        applicationAnswerRepository.findByApplicationId(application.getId()).stream()
            .map(ApplicationAnswerResult::from)
            .toList();

    return ApplicantDetailResult.of(application, answers);
  }

  /**
   * 7.5. 7.1 목록과 동일한 필터 · 정렬 기준({@link #APPLICANT_ORDER})으로 나열했을 때 현재
   * 지원서의 앞뒤 지원서 id 를 반환한다.
   *
   * <p>DRAFT 는 7.2 와 동일하게 없는 지원서처럼 404 로 처리한다 (PR #54 리뷰 지적 — 예전엔
   * {@code existsById} 만 써서 DRAFT 도 "존재"로 보고 200 + null/null 을 내려줬다).
   *
   * <p>현재 지원서가 (기수 · 상태) 필터에 안 맞으면 이전 · 다음 모두 null 이다 — "탐색할 게
   * 없다"는 에러가 아니므로 예외로 처리하지 않는다.
   *
   * <p>앞뒤 id 를 구하려고 기수 전체를 불러와 자바에서 인덱스를 세지 않는다. 현재 지원서의
   * (submittedAt, id) 를 커서로 삼아 바로 다음 · 이전 한 건만 DB 에 물어본다 (PR #54 리뷰 지적 —
   * 지원자가 수백 명이면 상세 화면을 넘길 때마다 전체 로딩 비용이 들었다).
   */
  public AdjacentApplicantResult getAdjacentApplicants(
      Long applicationId, Long generationId, ApplicationStatus status
  ) {
    Application application = findEvaluableApplication(applicationId);
    Long targetGenerationId = resolveGenerationId(generationId);

    boolean matchesFilter = application.getGenerationId().equals(targetGenerationId)
        && (status == null || application.getStatus() == status);
    if (!matchesFilter) {
      return new AdjacentApplicantResult(null, null);
    }

    Pageable top1 = PageRequest.of(0, 1);
    Long previousId = firstOrNull(status != null
        ? applicationRepository.findPreviousIdByGenerationIdAndStatus(
            targetGenerationId, status, application.getSubmittedAt(), application.getId(), top1)
        : applicationRepository.findPreviousIdByGenerationIdAndStatusNot(
            targetGenerationId, ApplicationStatus.DRAFT, application.getSubmittedAt(), application.getId(), top1));
    Long nextId = firstOrNull(status != null
        ? applicationRepository.findNextIdByGenerationIdAndStatus(
            targetGenerationId, status, application.getSubmittedAt(), application.getId(), top1)
        : applicationRepository.findNextIdByGenerationIdAndStatusNot(
            targetGenerationId, ApplicationStatus.DRAFT, application.getSubmittedAt(), application.getId(), top1));

    return new AdjacentApplicantResult(previousId, nextId);
  }

  private Long firstOrNull(List<Long> ids) {
    return ids.isEmpty() ? null : ids.get(0);
  }

  /**
   * 7.6. 7.1 목록과 동일한 필터로 페이징 없이 전체를 한 시트에 담는다. {@code ApiResponse}
   * envelope 을 쓰지 않는다 — 바이너리(XLSX) 응답이라 JSON 으로 감쌀 수 없다.
   */
  public byte[] exportApplicantsExcel(Long generationId, ApplicationStatus status) {
    List<Application> applications = findOrdered(resolveGenerationId(generationId), status);

    List<String> headers = List.of("이름", "학번", "상태", "제출일시");
    List<List<Object>> rows = applications.stream()
        .map(application -> List.<Object>of(
            application.getName(),
            application.getStudentNumber() != null ? application.getStudentNumber() : "",
            application.getStatus().label(),
            application.getSubmittedAt() != null ? application.getSubmittedAt().format(EXCEL_DATE_TIME_FORMAT) : ""))
        .toList();

    return ExcelExporter.toXlsx("지원자 목록", headers, rows);
  }

  /** 7.6 엑셀 다운로드 전용. 7.1 목록과 동일한 필터 · 정렬 기준으로 페이징 없이 전체 조회한다. */
  private List<Application> findOrdered(Long generationId, ApplicationStatus status) {
    return status != null
        ? applicationRepository.findByGenerationIdAndStatus(generationId, status, APPLICANT_ORDER)
        : applicationRepository.findByGenerationIdAndStatusNot(generationId, ApplicationStatus.DRAFT, APPLICANT_ORDER);
  }

  private Long resolveGenerationId(Long generationId) {
    return generationId != null ? generationId : findActiveGeneration().id();
  }

  /** 7.2 상세 · 7.5 순차탐색 공용. DRAFT 는 심사 대상이 아니므로 없는 지원서와 동일하게 404 로 처리한다. */
  private Application findEvaluableApplication(Long applicationId) {
    return applicationRepository.findById(applicationId)
        .filter(a -> a.getStatus() != ApplicationStatus.DRAFT)
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.APPLICATION_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }
}
