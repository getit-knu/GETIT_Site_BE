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
   *
   * <p>{@code criterion_id} 에 인덱스가 있어야 점수 전체를 훑지 않는다.
   * V28 에서 FK 와 함께 추가했다.
   *
   * <p>DB 에도 {@code ON DELETE CASCADE} 가 걸려 있다. 이 호출과 기준 삭제 사이에
   * 다른 트랜잭션이 점수를 새로 저장해도 고아가 남지 않는다. 여기서 먼저 지우는 것은
   * 영속성 컨텍스트를 DB 와 맞추고 삭제 건수를 남기기 위해서다.
   */
  long deleteByCriterionId(Long criterionId);
}
