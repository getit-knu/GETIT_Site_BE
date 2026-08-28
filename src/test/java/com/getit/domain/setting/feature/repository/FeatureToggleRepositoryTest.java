package com.getit.domain.setting.feature.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class FeatureToggleRepositoryTest {

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("enum 키로 저장하고 조회한다 (VARCHAR 왕복)")
  void savesAndFindsByEnumKey() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));
    // 영속성 컨텍스트를 비워야 findById 가 캐시된 엔티티가 아니라 DB 에서 enum 을 다시
    // 읽어와 VARCHAR 저장·복원 경로를 실제로 검증한다 (PR #102 리뷰 지적).
    entityManager.flush();
    entityManager.clear();

    FeatureToggle found = featureToggleRepository.findById(FeatureKey.STOCK_GAME).orElseThrow();

    assertThat(found.getToggleKey()).isEqualTo(FeatureKey.STOCK_GAME);
    assertThat(found.isEnabled()).isFalse();
  }
}
