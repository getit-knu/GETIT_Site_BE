package com.getit.domain.setting.feature.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.feature.dto.FeatureResult;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.exception.FeatureErrorCode;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FeatureToggleAdminServiceTest {

  @Autowired
  private FeatureToggleAdminService featureToggleAdminService;

  @Autowired
  private FeatureToggleRepository featureToggleRepository;

  @Autowired
  private UserRepository userRepository;

  @BeforeEach
  void seedToggles() {
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));
    featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, false));
  }

  @Nested
  @DisplayName("getFeatures")
  class GetFeatures {

    @Test
    @DisplayName("FeatureKey 선언 순서로 반환한다")
    void returnsInEnumOrder() {
      featureToggleRepository.deleteAll();
      featureToggleRepository.save(FeatureToggle.create(FeatureKey.MOCK_INVESTMENT, false));
      featureToggleRepository.save(FeatureToggle.create(FeatureKey.STOCK_GAME, false));

      List<FeatureResult> results = featureToggleAdminService.getFeatures();

      assertThat(results).extracting(FeatureResult::key)
          .containsExactly(FeatureKey.STOCK_GAME, FeatureKey.MOCK_INVESTMENT);
    }

    @Test
    @DisplayName("토글한 적 없으면 updatedBy 는 null 이다")
    void updatedByNullBeforeToggle() {
      List<FeatureResult> results = featureToggleAdminService.getFeatures();

      assertThat(results).allSatisfy(result -> assertThat(result.updatedBy()).isNull());
    }

    @Test
    @DisplayName("label 은 FeatureKey 에서 온다")
    void labelFromEnum() {
      List<FeatureResult> results = featureToggleAdminService.getFeatures();

      assertThat(results).extracting(FeatureResult::label)
          .containsExactly(FeatureKey.STOCK_GAME.getLabel(), FeatureKey.MOCK_INVESTMENT.getLabel());
    }
  }

  @Nested
  @DisplayName("updateFeature")
  class UpdateFeature {

    @Test
    @DisplayName("enabled 를 바꾸고 저장한다")
    void togglesEnabled() {
      FeatureResult result = featureToggleAdminService.updateFeature(FeatureKey.STOCK_GAME, true, 1L);

      assertThat(result.enabled()).isTrue();
      assertThat(featureToggleRepository.findById(FeatureKey.STOCK_GAME).orElseThrow().isEnabled())
          .isTrue();
    }

    @Test
    @DisplayName("updatedBy 를 활성 사용자 이름으로 해석한다")
    void resolvesUpdatedByName() {
      User admin = userRepository.save(User.createGuest("admin-1", "admin-1@getit.com", "관리자", null));

      FeatureResult result =
          featureToggleAdminService.updateFeature(FeatureKey.STOCK_GAME, true, admin.getId());

      assertThat(result.updatedBy()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("없는 사용자 id 면 updatedBy 는 UNKNOWN 이다")
    void unknownUpdaterBecomesUnknown() {
      FeatureResult result = featureToggleAdminService.updateFeature(FeatureKey.STOCK_GAME, true, 999L);

      assertThat(result.updatedBy()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("시드되지 않은 키면 예외가 발생한다")
    void throwsWhenNotSeeded() {
      featureToggleRepository.deleteById(FeatureKey.MOCK_INVESTMENT);

      assertThatThrownBy(
          () -> featureToggleAdminService.updateFeature(FeatureKey.MOCK_INVESTMENT, true, 1L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(FeatureErrorCode.FEATURE_NOT_FOUND);
    }
  }
}
