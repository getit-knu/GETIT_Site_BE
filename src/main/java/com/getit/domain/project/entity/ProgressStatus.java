package com.getit.domain.project.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 프로젝트 진행 상태. 공개 여부인 {@link ProjectStatus} 와는 별개다. (이슈 #221) */
@Getter
@RequiredArgsConstructor
public enum ProgressStatus {

  IN_PROGRESS("개발 중"),
  LIVE("서비스 중"),
  ENDED("서비스 종료");

  private final String label;
}
