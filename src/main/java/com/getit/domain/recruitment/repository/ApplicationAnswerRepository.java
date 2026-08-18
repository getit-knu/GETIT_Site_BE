package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.ApplicationAnswer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {

  /** 지원서에 달린 답변 전체 조회. (3.2) */
  List<ApplicationAnswer> findByApplicationId(Long applicationId);
}
