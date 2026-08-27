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
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
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
   */
  @Transactional
  public UserPromotionResult promote(Long generationId, List<Long> applicationIds) {
    GenerationSummary generation = findGeneration(generationId);

    List<ApplicationPromotionSummary> candidates = applicationIds == null
        ? applicationQueryService.findFinalPassByGenerationId(generation.id())
        : applicationQueryService.findFinalPassByIdsAndGenerationId(applicationIds, generation.id());

    List<PromotionSkipResult> skipped = new ArrayList<>();
    if (applicationIds != null) {
      skipped.addAll(findRequestedButNotEligible(applicationIds, candidates));
    }

    int promotedCount = 0;
    for (ApplicationPromotionSummary candidate : candidates) {
      PromotionSkipReason skipReason = tryPromote(candidate, generation.generationNo());
      if (skipReason != null) {
        skipped.add(new PromotionSkipResult(candidate.applicationId(), skipReason));
      } else {
        promotedCount++;
      }
    }

    return new UserPromotionResult(promotedCount, skipped.size(), skipped);
  }

  /** null 이면 승격 성공, 아니면 건너뛴 사유. */
  private PromotionSkipReason tryPromote(ApplicationPromotionSummary candidate, Integer generationNo) {
    User user = userRepository.findById(candidate.userId()).orElse(null);
    if (user == null || user.isDeleted()) {
      return PromotionSkipReason.USER_WITHDRAWN;
    }
    if (user.getRole() != Role.GUEST) {
      return PromotionSkipReason.ALREADY_MEMBER;
    }

    user.updateApplicantInfo(
        candidate.phoneNumber(),
        resolveCollegeName(candidate.collegeId()),
        resolveMajorName(candidate.majorId()),
        candidate.studentYear(),
        candidate.studentNumber());
    user.promoteToMember(generationNo);
    return null;
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

  /** College · Major 마스터 데이터는 이 프로젝트 관례대로 FK 값만 가지므로 직접 조회해서 이름을 뽑는다. */
  private String resolveCollegeName(Long collegeId) {
    return collegeId == null ? null : collegeRepository.findById(collegeId).map(College::getName).orElse(null);
  }

  private String resolveMajorName(Long majorId) {
    return majorId == null ? null : majorRepository.findById(majorId).map(Major::getName).orElse(null);
  }

  private GenerationSummary findGeneration(Long generationId) {
    return generationQueryService.findById(generationId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.GENERATION_NOT_FOUND));
  }
}
