package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.service.EventView;
import java.util.List;

public record EventCalendarResult(
    int year,
    int month,
    List<EventView> events
) { }
