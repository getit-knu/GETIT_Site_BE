package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Assignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

  Optional<Assignment> findByLectureId(Long lectureId);

  List<Assignment> findAllByLectureIdIn(List<Long> lectureIds);

  @Query("""
      select a from Assignment a
      where a.lectureId in :lectureIds and a.deadline >= :from
      order by a.deadline asc, a.id asc
      """)
  List<Assignment> findOngoingByLectureIds(@Param("lectureIds") List<Long> lectureIds, @Param("from") LocalDateTime from);
}
