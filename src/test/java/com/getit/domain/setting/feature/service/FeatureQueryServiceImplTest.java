package com.getit.domain.setting.feature.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FeatureQueryServiceImplTest {

  @Autowired
  private FeatureQueryService featureQueryService;

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @Test
  @DisplayName("모든 토글을 key 순으로 enabled 상태와 함께 반환한다")
  void returnsAllTogglesByKey() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, true));
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));

    List<FeatureView> result = featureQueryService.findAll();

    assertThat(result).containsExactly(
        new FeatureView(FeatureKey.STOCK_GAME, false),
        new FeatureView(FeatureKey.MOCK_INVESTMENT, true));
  }
}
