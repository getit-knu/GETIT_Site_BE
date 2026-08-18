package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Assignment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

  Optional<Assignment> findByLectureId(Long lectureId);
}
