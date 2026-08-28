package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Feedback;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

  List<Feedback> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

  @Query("select distinct f.submissionId from Feedback f where f.submissionId in :submissionIds")
  Set<Long> findSubmissionIdsWithFeedback(List<Long> submissionIds);
}
