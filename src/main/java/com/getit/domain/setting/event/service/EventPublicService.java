package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.dto.EventCalendarResult;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventPublicService {

  private final GenerationQueryService generationQueryService;
  private final EventQueryService eventQueryService;

  public EventCalendarResult getMonthly(int year, int month) {
    YearMonth target = YearMonth.of(year, month);
    List<EventCalendarResult.Item> events = generationQueryService.findActive()
        .map(GenerationSummary::generationNo)
        .map(generationNo -> eventQueryService.findByMonth(generationNo, target))
        .orElseGet(List::of)
        .stream()
        .map(EventCalendarResult.Item::from)
        .toList();
    return new EventCalendarResult(year, month, events);
  }
}
