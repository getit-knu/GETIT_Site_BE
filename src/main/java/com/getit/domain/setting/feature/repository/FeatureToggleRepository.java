package com.getit.domain.setting.feature.repository;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureToggleRepository extends JpaRepository<FeatureToggle, FeatureKey> {
}
