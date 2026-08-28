package com.getit.domain.setting.feature.service;

import com.getit.domain.setting.feature.dto.FeatureResult;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.exception.FeatureErrorCode;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 기능 토글 조회 · 갱신. (명세서 10.23 · 10.24) */
@Slf4j
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

    List<FeatureResult> results = new ArrayList<>();
    for (FeatureKey key : FeatureKey.values()) {
      FeatureToggle toggle = byKey.get(key);
      if (toggle == null) {
        log.warn("기능 토글 {} 의 시드 행이 없어 응답에서 제외한다. seed 마이그레이션 누락 여부를 확인하라.", key);
        continue;
      }
      results.add(FeatureResult.of(toggle, resolveUpdatedByName(toggle.getUpdatedBy())));
    }
    return results;
  }

  /** 10.24. */
  @Transactional
  public FeatureResult updateFeature(FeatureKey key, boolean enabled, Long updatedBy) {
    FeatureToggle toggle = featureToggleRepository.findById(key)
        .orElseThrow(() -> new BusinessException(FeatureErrorCode.FEATURE_NOT_FOUND));

    toggle.updateEnabled(enabled, updatedBy);
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
