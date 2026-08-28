package com.getit.domain.qna.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/** 응답 DTO의 시각 필드를 명세서 표기(KST, +09:00 오프셋)에 맞게 변환한다. */
public final class QnaDateTimes {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

  private QnaDateTimes() { }

  public static OffsetDateTime toOffset(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.atZone(ZONE_SEOUL).toOffsetDateTime();
  }
}
