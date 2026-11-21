package com.getit.domain.festa.service;

import java.time.OffsetDateTime;

/** 다른 도메인(홈)이 읽는 투표 기간. (이슈 #234) */
public record FestaScheduleView(OffsetDateTime startAt, OffsetDateTime endAt) { }
