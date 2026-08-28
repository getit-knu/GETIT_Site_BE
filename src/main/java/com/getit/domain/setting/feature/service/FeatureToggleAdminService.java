package com.getit.domain.setting.feature.service;

import com.getit.domain.setting.feature.dto.FeatureResult;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.exception.FeatureErrorCode;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기능 토글 조회 · 갱신. (명세서 10.23 · 10.24) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeatureToggleAdminService {

  private final FeatureToggleRepository featureToggleRepository;
  private final UserAccountService userAccountService;

  /** 10.23. FeatureKey 선언 순서로 반환한다. */
  public List<FeatureResult> getFeatures() {
    Map<FeatureKey, FeatureToggle> byKey = featureToggleRepository.findAll().stream()
        .collect(Collectors.toMap(FeatureToggle::getToggleKey, Function.identity()));

    return Arrays.stream(FeatureKey.values())
        .map(byKey::get)
        .filter(Objects::nonNull)
        .map(toggle -> FeatureResult.of(toggle, resolveUpdatedByName(toggle.getUpdatedBy())))
        .toList();
  }

  /** 10.24. */
  @Transactional
  public FeatureResult updateFeature(FeatureKey key, boolean enabled, Long updatedBy) {
    FeatureToggle toggle = featureToggleRepository.findById(key)
        .orElseThrow(() -> new BusinessException(FeatureErrorCode.FEATURE_NOT_FOUND));

    toggle.updateEnabled(enabled, updatedBy);
    // @LastModifiedDate 는 flush 시점에 갱신된다. 응답 DTO 를 만들기 전에 flush 해야 변경 후
    // updatedAt 이 담긴다 (FeedbackService.update 와 동일 — PR #102 리뷰 지적).
    featureToggleRepository.flush();

    return FeatureResult.of(toggle, resolveUpdatedByName(updatedBy));
  }

  private String resolveUpdatedByName(Long updatedBy) {
    if (updatedBy == null) {
      return null;
    }
    return userAccountService.findActiveById(updatedBy)
        .map(UserAccount::name)
        .orElse("UNKNOWN");
  }
}
