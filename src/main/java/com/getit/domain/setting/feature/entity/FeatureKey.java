package com.getit.domain.setting.feature.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 토글 가능한 기능. label 은 공개 화면 표기용. (명세서 0.5 · 10.23) */
@Getter
@RequiredArgsConstructor
public enum FeatureKey {

  STOCK_GAME("모의 주식 게임"),
  MOCK_INVESTMENT("모의 투자");

  private final String label;
}
