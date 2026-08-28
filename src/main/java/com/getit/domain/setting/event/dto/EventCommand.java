package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.entity.EventType;
import java.time.LocalDate;

public record EventCommand(
    String title,
    String place,
    LocalDate startDate,
    LocalDate endDate,
    boolean isVisible,
    EventType type
) { }
