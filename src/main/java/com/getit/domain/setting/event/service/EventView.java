package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.entity.EventType;
import java.time.LocalDate;

public record EventView(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    EventType type,
    String place
) { }
