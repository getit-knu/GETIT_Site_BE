package com.getit.domain.setting.feature.service;

import com.getit.domain.setting.feature.entity.FeatureKey;

public record FeatureView(
    FeatureKey key,
    boolean enabled
) { }
