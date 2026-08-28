package com.getit.domain.setting.feature.dto;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import java.time.LocalDateTime;

/** 기능 토글 조회 · 갱신 결과. (명세서 10.23 · 10.24) */
public record FeatureResult(
    FeatureKey key,
    String label,
    boolean enabled,
    LocalDateTime updatedAt,
    String updatedBy
) {

  public static FeatureResult of(FeatureToggle toggle, String updatedByName) {
    return new FeatureResult(
        toggle.getToggleKey(),
        toggle.getToggleKey().getLabel(),
        toggle.isEnabled(),
        toggle.getUpdatedAt(),
        updatedByName
    );
  }
}
