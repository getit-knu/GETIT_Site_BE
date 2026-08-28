package com.getit.domain.setting.feature.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// enum 추가하시면 seed 넣으셔야 합니다~
@Getter
@RequiredArgsConstructor
public enum FeatureKey {

  STOCK_GAME("모의 주식 게임"),
  MOCK_INVESTMENT("모의 투자");

  private final String label;
}
