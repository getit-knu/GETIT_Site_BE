package com.getit.domain.festa.service;

import com.getit.domain.festa.entity.FestaInvestor;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaInvestorRepository;
import com.getit.domain.project.service.ProjectQueryService;
import com.getit.domain.project.service.ProjectView;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureQueryService;
import com.getit.domain.user.dto.GroupWithMembersResult;
import com.getit.domain.user.service.MemberGroupService;
import com.getit.global.exception.BusinessException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 투자와 투표가 함께 쓰는 참여 규칙: 토글, 출전 여부, 우리 조, 전화번호. (이슈 #232)
 *
 * <p>호출하는 서비스의 트랜잭션에 참여한다.
 */
@Component
@RequiredArgsConstructor
class FestaParticipation {

  private static final Pattern PHONE_NUMBER = Pattern.compile("010-\\d{4}-\\d{4}");

  private final FestaInvestorRepository investorRepository;
  private final FeatureQueryService featureQueryService;
  private final ProjectQueryService projectQueryService;
  private final MemberGroupService memberGroupService;
  private final Clock clock;

  void ensureOpen() {
    boolean open = featureQueryService.findAll().stream()
        .anyMatch(feature -> feature.key() == FeatureKey.MOCK_INVESTMENT && feature.enabled());
    if (!open) {
      throw new BusinessException(FestaErrorCode.FESTA_CLOSED);
    }
  }

  /** 출전하지 않았거나 우리 조 프로젝트면 거부한다. */
  void validateProject(Long userId, Long projectId) {
    ProjectView project = projectQueryService.findFestaEntries().stream()
        .filter(entry -> entry.id().equals(projectId))
        .findFirst()
        .orElseThrow(() -> new BusinessException(FestaErrorCode.PROJECT_NOT_ENTERED));
    Long myGroupId = findMyGroupId(userId);
    if (myGroupId != null && myGroupId.equals(project.groupId())) {
      throw new BusinessException(FestaErrorCode.OWN_GROUP_PROJECT);
    }
  }

  /** 번호를 보내면 형식과 동의를 확인해 바꾸고, 안 보내면 저장된 번호를 유지한다. 첫 저장에는 필수다. */
  void savePhoneNumber(Long userId, String rawPhoneNumber, Boolean privacyConsent) {
    FestaInvestor existing = investorRepository.findByUserId(userId).orElse(null);
    String phoneNumber = rawPhoneNumber == null ? "" : rawPhoneNumber.trim();

    if (phoneNumber.isEmpty()) {
      if (existing == null || !existing.hasPhoneNumber()) {
        throw new BusinessException(FestaErrorCode.PHONE_NUMBER_REQUIRED);
      }
      return;
    }
    if (!PHONE_NUMBER.matcher(phoneNumber).matches()) {
      throw new BusinessException(FestaErrorCode.INVALID_PHONE_NUMBER);
    }
    if (!Boolean.TRUE.equals(privacyConsent)) {
      throw new BusinessException(FestaErrorCode.PRIVACY_CONSENT_REQUIRED);
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (existing == null) {
      investorRepository.save(FestaInvestor.of(userId, phoneNumber, now));
    } else {
      existing.registerPhone(phoneNumber, now);
    }
  }

  String maskedPhoneNumber(Long userId) {
    return investorRepository.findByUserId(userId)
        .filter(FestaInvestor::hasPhoneNumber)
        .map(investor -> mask(investor.getPhoneNumber()))
        .orElse(null);
  }

  List<Long> ownProjectIds(Long userId) {
    Long myGroupId = findMyGroupId(userId);
    if (myGroupId == null) {
      return List.of();
    }
    return projectQueryService.findFestaEntries().stream()
        .filter(project -> myGroupId.equals(project.groupId()))
        .map(ProjectView::id)
        .toList();
  }

  private Long findMyGroupId(Long userId) {
    return memberGroupService.findMyGroup(userId).map(GroupWithMembersResult::id).orElse(null);
  }

  /** 010-1234-5678 → 010-****-5678 */
  static String mask(String phoneNumber) {
    return phoneNumber.substring(0, 4) + "****" + phoneNumber.substring(8);
  }
}
