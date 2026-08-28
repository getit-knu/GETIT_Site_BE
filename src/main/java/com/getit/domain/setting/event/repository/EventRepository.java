package com.getit.domain.setting.event.repository;

import com.getit.domain.setting.event.entity.Event;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

  List<Event> findByGenerationIdOrderByStartDateAscIdAsc(long generationId);

  Optional<Event> findByIdAndGenerationId(Long id, long generationId);
}
