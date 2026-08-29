package com.getit.domain.setting.home.dto;

import java.time.OffsetDateTime;

/** 홈 화면 일괄 저장 결과. (API 명세서 10.20) */
public record HomeSaveResult(
    OffsetDateTime savedAt,
    Integer generationNo
) { }
