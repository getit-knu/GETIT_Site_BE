package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

  Optional<AssignmentSubmission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);

  List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);
}
