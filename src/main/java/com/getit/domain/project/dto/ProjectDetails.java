package com.getit.domain.project.dto;

import com.getit.domain.project.entity.ProgressStatus;

/** 프로젝트 필드 확장분. 모두 선택값이다. (이슈 #221) */
public record ProjectDetails(
    String oneLiner,
    ProgressStatus progressStatus,
    String playStoreUrl,
    String appStoreUrl
) {

  public static final ProjectDetails EMPTY = new ProjectDetails(null, null, null, null);
}
