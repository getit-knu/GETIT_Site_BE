package com.getit.domain.festa.dto;

import java.time.OffsetDateTime;

/** 투표 기간. 미설정이면 둘 다 null. (이슈 #234) */
public record FestaScheduleResult(OffsetDateTime startAt, OffsetDateTime endAt) { }
