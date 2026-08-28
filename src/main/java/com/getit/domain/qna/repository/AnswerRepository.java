package com.getit.domain.qna.repository;

import com.getit.domain.qna.entity.Answer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

  Optional<Answer> findByQuestionId(long questionId);

  boolean existsByQuestionId(long questionId);

  List<Answer> findAllByQuestionIdIn(List<Long> questionIds);
}
