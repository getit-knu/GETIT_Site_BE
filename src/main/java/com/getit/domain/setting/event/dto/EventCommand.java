package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.exception.EventErrorCode;
import com.getit.global.exception.BusinessException;
import java.time.LocalDate;

public record EventCommand(
    String title,
    String place,
    LocalDate startDate,
    LocalDate endDate,
    boolean isVisible,
    EventType type
) {

  public EventCommand {
    if (startDate.isAfter(endDate)) {
      throw new BusinessException(EventErrorCode.INVALID_EVENT_PERIOD);
    }
  }
}
