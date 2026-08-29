package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.entity.EventType;
import java.time.LocalDate;

public record EventUpsert(
    Long id,
    String title,
    String place,
    LocalDate startDate,
    LocalDate endDate,
    EventType type,
    boolean isVisible
) { }
