package com.getit.domain.qna.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QnaStatus {

  PENDING("답변 대기"),
  ANSWERED("답변 완료");

  private final String label;
}
