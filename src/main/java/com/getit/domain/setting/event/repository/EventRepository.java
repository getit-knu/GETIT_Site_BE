package com.getit.domain.setting.event.repository;

import com.getit.domain.setting.event.entity.Event;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

  List<Event> findByGenerationIdOrderByStartDateAscIdAsc(long generationId);

  Optional<Event> findByIdAndGenerationId(Long id, long generationId);

  @Query("""
      select e from Event e
      where e.generationId = :generationId
        and e.isVisible = true
        and e.startDate <= :monthEnd
        and e.endDate >= :monthStart
      order by e.startDate asc, e.id asc
      """)
  List<Event> findVisibleOverlapping(
      @Param("generationId") long generationId,
      @Param("monthStart") LocalDate monthStart,
      @Param("monthEnd") LocalDate monthEnd
  );

  @Query("""
      select e from Event e
      where e.generationId = :generationId
        and e.isVisible = true
        and e.startDate >= :from
      order by e.startDate asc, e.id asc
      """)
  List<Event> findVisibleUpcoming(@Param("generationId") long generationId, @Param("from") LocalDate from);
}
