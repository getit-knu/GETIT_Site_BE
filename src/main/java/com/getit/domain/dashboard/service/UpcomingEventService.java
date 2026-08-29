package com.getit.domain.dashboard.service;

import com.getit.domain.dashboard.dto.UpcomingEventResult;
import com.getit.domain.setting.event.service.EventQueryService;
import com.getit.domain.setting.event.service.EventView;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 행사 일정 D-day. (API 명세서 5.4) 활성 기수가 없으면 빈 리스트로 응답한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UpcomingEventService {

  private final GenerationQueryService generationQueryService;
  private final EventQueryService eventQueryService;
  private final Clock clock;

  public List<UpcomingEventResult> getUpcomingEvents() {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();
    if (activeGeneration.isEmpty()) {
      return List.of();
    }

    LocalDate today = LocalDate.now(clock);
    return eventQueryService.findUpcoming(activeGeneration.get().generationNo()).stream()
        .map(event -> toResult(event, today))
        .toList();
  }

  private UpcomingEventResult toResult(EventView event, LocalDate today) {
    return new UpcomingEventResult(
        event.id(), event.title(), event.place(), event.startDate(),
        ChronoUnit.DAYS.between(today, event.startDate()), event.type());
  }
}
