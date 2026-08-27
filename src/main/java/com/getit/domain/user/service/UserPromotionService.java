package com.getit.domain.user.service;

import com.getit.domain.recruitment.dto.ApplicationPromotionSummary;
import com.getit.domain.recruitment.service.ApplicationQueryService;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.PromotionSkipReason;
import com.getit.domain.user.dto.PromotionSkipResult;
import com.getit.domain.user.dto.UserPromotionResult;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 합격자 일괄 승격. (API 명세서 9.4)
 *
 * <p>{@code recruitment.Application} 은 {@link ApplicationQueryService} 를 거쳐서만 읽는다
 * (작업 분할 계획 4.2 크로스 도메인 계약).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserPromotionService {

  private final UserRepository userRepository;
  private final CollegeRepository collegeRepository;
  private final MajorRepository majorRepository;
  private final ApplicationQueryService applicationQueryService;
  private final GenerationQueryService generationQueryService;

  /**
   * {@code applicationIds} 가 없으면 대상 기수의 FINAL_PASS 전체, 있으면 그 중 실제로 같은 기수·
   * FINAL_PASS 인 것만 승격한다. 대상에서 빠진 사유는 전부 {@code skipped} 로 모은다 — 일부 실패로
   * 전체를 롤백하지 않는다(명세서 9.4 응답이 promotedCount·skippedCount 를 함께 반환하는 것도
   * 이 때문이다).
   *
   * <p>{@code generationId} 는 현재 활성 기수와 일치해야 한다 — id 존재 여부만 확인하면 비활성
   * (과거) 기수의 FINAL_PASS 지원서로도 사용자를 승격하고 소속 기수를 바꿀 수 있다(PR #69 Copilot
   * 리뷰 지적 — {@code GroupService.findGroup} · {@code EvaluationCriterionService.findCriterion}
   * 과 동일한 "기수 소속 리소스 변경은 활성 기수로 제한" 패턴).
   */
  @Transactional
  public UserPromotionResult promote(Long generationId, List<Long> applicationIds) {
    GenerationSummary activeGeneration = findActiveGenerationMatching(generationId);

    List<ApplicationPromotionSummary> candidates = applicationIds == null
        ? applicationQueryService.findFinalPassByGenerationId(activeGeneration.id())
        : applicationQueryService.findFinalPassByIdsAndGenerationId(applicationIds, activeGeneration.id());

    List<PromotionSkipResult> skipped = new ArrayList<>();
    if (applicationIds != null) {
      skipped.addAll(findRequestedButNotEligible(applicationIds, candidates));
    }

    Map<Long, String> collegeNames = findCollegeNames(candidates);
    Map<Long, String> majorNames = findMajorNames(candidates);

    int promotedCount = 0;
    for (ApplicationPromotionSummary candidate : candidates) {
      PromotionSkipReason skipReason =
          tryPromote(candidate, activeGeneration.generationNo(), collegeNames, majorNames);
      if (skipReason != null) {
        skipped.add(new PromotionSkipResult(candidate.applicationId(), skipReason));
      } else {
        promotedCount++;
      }
    }

    return new UserPromotionResult(promotedCount, skipped.size(), skipped);
  }

  /**
   * null 이면 승격 성공, 아니면 건너뛴 사유.
   *
   * <p>승격 반영은 {@link UserRepository#promoteIfEligible} 원자적 UPDATE 하나로 한다 — "GUEST ·
   * 탈퇴 여부 확인 후 변경"을 자바에서 두 단계로 하면, 그 사이에 다른 요청이 같은 사용자를 먼저
   * 승격·탈퇴·강등시켜 중복 집계나 덮어쓰기가 생길 수 있다(PR #69 Copilot suppressed 리뷰 지적 —
   * {@code User} 에 낙관적 잠금이 없어서 더욱 그렇다).
   */
  private PromotionSkipReason tryPromote(
      ApplicationPromotionSummary candidate,
      Integer generationNo,
      Map<Long, String> collegeNames,
      Map<Long, String> majorNames
  ) {
    int updated = userRepository.promoteIfEligible(
        candidate.userId(),
        Role.MEMBER,
        Role.GUEST,
        generationNo,
        candidate.phoneNumber(),
        collegeNames.get(candidate.collegeId()),
        majorNames.get(candidate.majorId()),
        candidate.studentYear(),
        candidate.studentNumber());
    return updated > 0 ? null : classifySkipReason(candidate.userId());
  }

  /**
   * 원자적 UPDATE 가 0행을 반영했을 때(이미 GUEST 가 아니거나 탈퇴함) 그 사유를 판별하는 용도로만
   * 쓴다. 이 조회는 판정 전용이라 UPDATE 이후 아주 좁은 경합 창이 남는다 — 조회 직후 다른 요청이
   * 상태를 또 바꾸면 스킵 사유 표기가 실제와 한 틱 어긋날 수 있다. 다만 실제 승격 반영은 이미
   * 원자적으로 끝난 뒤라 중복 집계·덮어쓰기 같은 손상은 생기지 않는다 — 여기서 어긋날 수 있는 건
   * 응답에 담기는 스킵 사유 표기뿐이다.
   */
  private PromotionSkipReason classifySkipReason(Long userId) {
    return userRepository.findById(userId)
        .map(user -> user.isDeleted() ? PromotionSkipReason.USER_WITHDRAWN : PromotionSkipReason.ALREADY_MEMBER)
        .orElse(PromotionSkipReason.USER_WITHDRAWN);
  }

  private List<PromotionSkipResult> findRequestedButNotEligible(
      List<Long> requestedIds, List<ApplicationPromotionSummary> eligible
  ) {
    Set<Long> eligibleIds = eligible.stream()
        .map(ApplicationPromotionSummary::applicationId)
        .collect(Collectors.toSet());
    return requestedIds.stream()
        .filter(id -> !eligibleIds.contains(id))
        .map(id -> new PromotionSkipResult(id, PromotionSkipReason.NOT_FINAL_PASS))
        .toList();
  }

  /**
   * College · Major 마스터 데이터는 이 프로젝트 관례대로 FK 값만 가지므로 직접 조회해서 이름을
   * 뽑는다. 후보마다 단건 조회하면 대상이 늘수록(FINAL_PASS 전체 처리 가능) 쿼리 수가 급증하므로
   * (PR #69 Copilot 리뷰 지적) id 를 모아 {@code findAllById} 로 한 번에 조회해 Map 으로 매핑한다.
   */
  private Map<Long, String> findCollegeNames(List<ApplicationPromotionSummary> candidates) {
    Set<Long> collegeIds = candidates.stream()
        .map(ApplicationPromotionSummary::collegeId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    return collegeRepository.findAllById(collegeIds).stream()
        .collect(Collectors.toMap(College::getId, College::getName));
  }

  private Map<Long, String> findMajorNames(List<ApplicationPromotionSummary> candidates) {
    Set<Long> majorIds = candidates.stream()
        .map(ApplicationPromotionSummary::majorId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    return majorRepository.findAllById(majorIds).stream()
        .collect(Collectors.toMap(Major::getId, Major::getName));
  }

  /** 활성 기수를 조회하고, 요청받은 {@code generationId} 가 그 활성 기수와 일치하는지 확인한다. */
  private GenerationSummary findActiveGenerationMatching(Long generationId) {
    GenerationSummary activeGeneration = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(UserErrorCode.ACTIVE_GENERATION_NOT_FOUND));
    if (!activeGeneration.id().equals(generationId)) {
      throw new BusinessException(UserErrorCode.GENERATION_NOT_FOUND);
    }
    return activeGeneration;
  }
}
