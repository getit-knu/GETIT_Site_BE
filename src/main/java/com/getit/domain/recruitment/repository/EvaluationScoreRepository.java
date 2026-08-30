package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.EvaluationScore;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

  /** 지원서에 매겨진 점수 전체 조회. (7.2 상세 · 7.3 저장) */
  List<EvaluationScore> findByApplicationId(Long applicationId);

  /** upsert(7.3) 시 이미 저장된 점수가 있는지 확인하는 데 쓴다. */
  Optional<EvaluationScore> findByApplicationIdAndCriterionIdAndEvaluatorId(
      Long applicationId, Long criterionId, Long evaluatorId);
}
