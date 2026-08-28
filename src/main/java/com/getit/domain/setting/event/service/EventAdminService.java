package com.getit.domain.setting.event.service;

import com.getit.domain.setting.event.dto.EventRequest;
import com.getit.domain.setting.event.dto.EventResult;
import com.getit.domain.setting.event.entity.Event;
import com.getit.domain.setting.event.exception.EventErrorCode;
import com.getit.domain.setting.event.repository.EventRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAdminService {

  private final EventRepository eventRepository;
  private final GenerationQueryService generationQueryService;

  public List<EventResult> getEvents() {
    GenerationSummary activeGeneration = findActiveGeneration();

    return eventRepository.findByGenerationIdOrderByStartDateAscIdAsc(activeGeneration.id()).stream()
        .map(EventResult::from)
        .toList();
  }

  @Transactional
  public EventResult createEvent(EventRequest request) {
    GenerationSummary activeGeneration = validateActiveGeneration(request.generationId());
    validatePeriod(request);

    Event saved = eventRepository.save(Event.create(request.toCommand(), activeGeneration.id()));

    return EventResult.from(saved);
  }

  @Transactional
  public EventResult updateEvent(Long eventId, EventRequest request) {
    GenerationSummary activeGeneration = validateActiveGeneration(request.generationId());
    validatePeriod(request);
    Event event = findEvent(eventId, activeGeneration.id());

    event.update(request.toCommand());

    return EventResult.from(event);
  }

  @Transactional
  public void deleteEvent(Long eventId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    Event event = findEvent(eventId, activeGeneration.id());

    eventRepository.delete(event);
  }

  private void validatePeriod(EventRequest request) {
    if (request.startDate().isAfter(request.endDate())) {
      throw new BusinessException(EventErrorCode.INVALID_EVENT_PERIOD);
    }
  }

  private Event findEvent(Long eventId, long activeGenerationId) {
    return eventRepository.findByIdAndGenerationId(eventId, activeGenerationId)
        .orElseThrow(() -> new BusinessException(EventErrorCode.EVENT_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(EventErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  private GenerationSummary validateActiveGeneration(Long generationId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    if (!activeGeneration.id().equals(generationId)) {
      throw new BusinessException(EventErrorCode.GENERATION_NOT_FOUND);
    }
    return activeGeneration;
  }
}
