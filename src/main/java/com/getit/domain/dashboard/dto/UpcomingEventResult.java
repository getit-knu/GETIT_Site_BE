package com.getit.domain.dashboard.dto;

import com.getit.domain.setting.event.entity.EventType;
import java.time.LocalDate;

/** 행사 일정 D-day. (API 명세서 5.4) */
public record UpcomingEventResult(
    Long id,
    String title,
    String place,
    LocalDate startDate,
    long dDay,
    EventType type
) { }
