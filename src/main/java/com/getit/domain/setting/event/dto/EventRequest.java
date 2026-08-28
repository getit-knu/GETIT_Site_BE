package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.entity.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EventRequest(
    @NotNull Long generationId,
    @NotBlank @Size(max = 100) String title,
    @NotBlank @Size(max = 100) String place,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotNull EventType type,
    @NotNull Boolean isVisible
) {

  public EventCommand toCommand() {
    return new EventCommand(title, place, startDate, endDate, isVisible, type);
  }
}
