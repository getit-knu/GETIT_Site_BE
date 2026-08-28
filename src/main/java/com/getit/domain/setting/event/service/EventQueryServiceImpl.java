package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

  private final EventRepository eventRepository;
  private final GenerationQueryService generationQueryService;

  @Override
  public List<EventView> findByMonth(int generationNo, YearMonth month) {
    Long generationId = resolveGenerationId(generationNo);
    if (generationId == null) {
      return List.of();
    }
    return eventRepository.findVisibleOverlapping(generationId, month.atDay(1), month.atEndOfMonth()).stream()
        .map(this::toView)
        .toList();
  }

  @Override
  public List<EventView> findUpcoming(int generationNo) {
    Long generationId = resolveGenerationId(generationNo);
    if (generationId == null) {
      return List.of();
    }
    return eventRepository.findUpcoming(generationId, LocalDate.now(ZONE_SEOUL)).stream()
        .map(this::toView)
        .toList();
  }

  private Long resolveGenerationId(int generationNo) {
    return generationQueryService.findByGenerationNo(generationNo).map(GenerationSummary::id).orElse(null);
  }

  private EventView toView(Event event) {
    return new EventView(event.getId(), event.getTitle(), event.getStartDate(),
        event.getEndDate(), event.getType(), event.getPlace());
  }
}
