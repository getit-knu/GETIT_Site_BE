package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.entity.EventType;
import java.time.LocalDate;

public record EventResult(
    Long id,
    String title,
    LocalDate startDate,
    LocalDate endDate,
    EventType type,
    String place,
    boolean isVisible
) {

  public static EventResult from(Event event) {
    return new EventResult(
        event.getId(),
        event.getTitle(),
        event.getStartDate(),
        event.getEndDate(),
        event.getType(),
        event.getPlace(),
        event.isVisible()
    );
  }
}
