package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EventQueryServiceImpl implements EventQueryService {

  private final EventRepository eventRepository;
  private final GenerationQueryService generationQueryService;

  /**
   * "다가오는" 의 기준 시각. (이슈 #207)
   *
   * <p>여기서 {@code LocalDate.now()} 를 직접 부르면, 호출하는 쪽이 시계를 고정해도 이 조회만
   * 실제 날짜로 나간다. 실제로 {@code UpcomingEventService} 는 {@code Clock} 을 주입받고
   * 있었는데도 테스트가 날짜에 따라 깨졌다 — <b>시계가 서비스 경계에서 끊겼다.</b>
   */
  private final Clock clock;

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
    return eventRepository.findUpcoming(generationId, LocalDate.now(clock)).stream()
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
