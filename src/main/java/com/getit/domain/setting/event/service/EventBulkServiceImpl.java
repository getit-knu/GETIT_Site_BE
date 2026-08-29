package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.dto.EventCommand;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.exception.EventErrorCode;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EventBulkServiceImpl implements EventBulkService {

  private final EventRepository eventRepository;
  private final GenerationQueryService generationQueryService;

  @Override
  public void replaceAll(int generationNo, List<EventUpsert> desired) {
    long generationId = generationQueryService.findByGenerationNo(generationNo)
        .map(GenerationSummary::id)
        .orElseThrow(() -> new BusinessException(EventErrorCode.ACTIVE_GENERATION_NOT_FOUND));

    Map<Long, Event> existing = eventRepository.findByGenerationIdForUpdate(generationId).stream()
        .collect(Collectors.toMap(Event::getId, Function.identity()));
    Set<Long> keepIds = desired.stream()
        .map(EventUpsert::id)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    eventRepository.deleteAll(existing.values().stream()
        .filter(event -> !keepIds.contains(event.getId()))
        .toList());

    for (EventUpsert item : desired) {
      EventCommand command = new EventCommand(
          item.title(), item.place(), item.startDate(), item.endDate(), item.isVisible(), item.type());
      if (item.id() == null) {
        eventRepository.save(Event.create(command, generationId));
        continue;
      }
      Event event = existing.get(item.id());
      if (event == null) {
        throw new BusinessException(EventErrorCode.EVENT_NOT_FOUND);
      }
      event.update(command);
    }
  }
}
