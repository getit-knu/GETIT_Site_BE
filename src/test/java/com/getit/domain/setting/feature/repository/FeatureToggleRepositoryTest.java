package com.getit.domain.setting.feature.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class FeatureToggleRepositoryTest {

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @Test
  @DisplayName("enum 키로 저장하고 조회한다")
  void savesAndFindsByEnumKey() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));

    FeatureToggle found = featureToggleRepository.findById(FeatureKey.STOCK_GAME).orElseThrow();

    assertThat(found.getToggleKey()).isEqualTo(FeatureKey.STOCK_GAME);
    assertThat(found.isEnabled()).isFalse();
  }
}
