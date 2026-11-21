package com.getit.domain.festa.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/** 투표 기간 저장 요청. startAt 이 endAt 보다 앞서야 한다. (이슈 #234) */
public record FestaScheduleRequest(@NotNull OffsetDateTime startAt, @NotNull OffsetDateTime endAt) { }
