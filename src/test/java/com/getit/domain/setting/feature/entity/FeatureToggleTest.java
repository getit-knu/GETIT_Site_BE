package com.getit.domain.setting.feature.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FeatureToggleTest {

  @Test
  @DisplayName("생성 시 updatedBy 는 비어 있다")
  void createsWithoutUpdater() {
    FeatureToggle toggle = FeatureToggle.create(FeatureKey.STOCK_GAME, false);

    assertThat(toggle.getToggleKey()).isEqualTo(FeatureKey.STOCK_GAME);
    assertThat(toggle.isEnabled()).isFalse();
    assertThat(toggle.getUpdatedBy()).isNull();
  }

  @Test
  @DisplayName("토글하면 enabled 와 updatedBy 가 바뀐다")
  void updatesEnabledAndUpdater() {
    FeatureToggle toggle = FeatureToggle.create(FeatureKey.STOCK_GAME, false);

    toggle.updateEnabled(true, 7L);

    assertThat(toggle.isEnabled()).isTrue();
    assertThat(toggle.getUpdatedBy()).isEqualTo(7L);
  }
}
