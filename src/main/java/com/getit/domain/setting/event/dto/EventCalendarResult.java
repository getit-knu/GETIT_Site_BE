package com.getit.domain.setting.event.dto;

import com.getit.domain.setting.event.entity.EventType;
import com.getit.domain.setting.event.service.EventView;
import java.time.LocalDate;
import java.util.List;

public record EventCalendarResult(
    int year,
    int month,
    List<Item> events
) {

  public record Item(
      Long id,
      String title,
      LocalDate startDate,
      LocalDate endDate,
      EventType type,
      String place
  ) {

    public static Item from(EventView view) {
      return new Item(view.id(), view.title(), view.startDate(), view.endDate(), view.type(), view.place());
    }
  }
}
