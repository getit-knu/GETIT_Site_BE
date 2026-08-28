package com.getit.domain.setting.feature.dto;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 기능 토글 조회 · 갱신 결과. (명세서 10.23 · 10.24) */
public record FeatureResult(
    FeatureKey key,
    String label,
    boolean enabled,
    OffsetDateTime updatedAt,
    String updatedBy
) {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

  public static FeatureResult of(FeatureToggle toggle, String updatedByName) {
    return new FeatureResult(
        toggle.getToggleKey(),
        toggle.getToggleKey().getLabel(),
        toggle.isEnabled(),
        toOffset(toggle.getUpdatedAt()),
        updatedByName
    );
  }

  private static OffsetDateTime toOffset(LocalDateTime value) {
    return value == null ? null : value.atZone(ZONE_SEOUL).toOffsetDateTime();
  }
}
