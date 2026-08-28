package com.getit.domain.setting.feature.service;

import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FeatureQueryServiceImpl implements FeatureQueryService {

  private final FeatureToggleRepository featureToggleRepository;

  @Override
  public List<FeatureView> findAll() {
    Map<FeatureKey, Boolean> enabledByKey = featureToggleRepository.findAll().stream()
        .collect(Collectors.toMap(FeatureToggle::getToggleKey, FeatureToggle::isEnabled));
    return Arrays.stream(FeatureKey.values())
        .map(key -> new FeatureView(key, enabledByKey.getOrDefault(key, false)))
        .toList();
  }
}
