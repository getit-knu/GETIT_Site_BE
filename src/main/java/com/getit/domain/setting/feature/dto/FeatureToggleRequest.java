package com.getit.domain.setting.feature.dto;

import jakarta.validation.constraints.NotNull;

/** 기능 토글 요청. (명세서 10.24) */
public record FeatureToggleRequest(@NotNull Boolean enabled) { }
