package com.getit.domain.setting.feature.service;

import com.getit.domain.setting.feature.entity.FeatureToggle;
import com.getit.domain.setting.feature.repository.FeatureToggleRepository;
import java.util.Comparator;
import java.util.List;
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
    return featureToggleRepository.findAll().stream()
        .sorted(Comparator.comparing(FeatureToggle::getToggleKey))
        .map(toggle -> new FeatureView(toggle.getToggleKey(), toggle.isEnabled()))
        .toList();
  }
}
