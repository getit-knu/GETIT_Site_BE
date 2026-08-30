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

  /**
   * 기준을 지울 때 그 기준의 점수를 함께 지운다. (6.11)
   *
   * <p>파생 삭제라 엔티티를 읽어 지운다. 벌크 삭제 쿼리로 만들면
   * {@code clearAutomatically} 를 붙여야 하고, 그러면 호출한 쪽이 들고 있던
   * 엔티티가 detached 가 되어 뒤이은 변경이 조용히 사라진다 (이슈 #160).
   */
  long deleteByCriterionId(Long criterionId);
}
