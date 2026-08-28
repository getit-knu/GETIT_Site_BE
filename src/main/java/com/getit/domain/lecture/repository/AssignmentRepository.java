package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Assignment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

  Optional<Assignment> findByLectureId(Long lectureId);

  List<Assignment> findAllByLectureIdIn(List<Long> lectureIds);

  List<Assignment> findByLectureIdInAndDeadlineGreaterThanEqualOrderByDeadlineAscIdAsc(
      List<Long> lectureIds, LocalDateTime from);
}
