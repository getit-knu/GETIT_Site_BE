package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Feedback;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

  List<Feedback> findAllBySubmissionIdOrderByIdAsc(Long submissionId);

  List<Feedback> findAllBySubmissionIdIn(List<Long> submissionIds);
}
