package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.AssignmentSubmission;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {

  Optional<AssignmentSubmission> findByAssignmentIdAndUserId(Long assignmentId, Long userId);

  List<AssignmentSubmission> findAllByAssignmentId(Long assignmentId);

  List<AssignmentSubmission> findAllByAssignmentIdIn(List<Long> assignmentIds);

  List<AssignmentSubmission> findAllByAssignmentIdInAndUserId(List<Long> assignmentIds, Long userId);

  @Query("""
      select count(s) from AssignmentSubmission s
      where s.assignmentId in (select a.id from Assignment a where a.lectureId in :lectureIds)
        and not exists (select 1 from Feedback f where f.submissionId = s.id)
      """)
  long countUnEvaluatedInLectures(@Param("lectureIds") List<Long> lectureIds);

  @Query("""
      select s.assignmentId as assignmentId, count(s) as count
      from AssignmentSubmission s
      where s.assignmentId in :assignmentIds
      group by s.assignmentId
      """)
  List<AssignmentSubmissionCount> countByAssignmentIdsGrouped(@Param("assignmentIds") List<Long> assignmentIds);

  interface AssignmentSubmissionCount {
    Long getAssignmentId();

    Long getCount();
  }
}
