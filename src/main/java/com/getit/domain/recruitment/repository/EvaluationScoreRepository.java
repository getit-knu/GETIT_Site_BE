package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationScore;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationScoreRepository extends JpaRepository<EvaluationScore, Long> {

  /** 지원서에 매겨진 점수 전체 조회. (7.2 상세 · 7.3 저장) */
  List<EvaluationScore> findByApplicationId(Long applicationId);

  /**
   * 여러 지원서의 점수를 한 번에 읽는다. (이슈 #188)
   *
   * <p>목록(7.1)에 점수를 실으려고 줄마다 조회하면 지원자 수만큼 쿼리가 나간다
   * (이슈 #142 에서 단과대 이름에 같은 문제가 있었다).
   */
  List<EvaluationScore> findByApplicationIdIn(Collection<Long> applicationIds);

  /**
   * 한 기수의 제출된 지원서 전체의 점수. 전체 평균을 낼 때 쓴다. (이슈 #188)
   *
   * <p>목록은 페이징되므로 현재 페이지로 평균을 내면 페이지를 넘길 때마다 기준값이 달라진다.
   * 비교 기준은 흔들리면 안 되므로 언제나 지원자 전체에서 낸다.
   */
  @Query("select s from EvaluationScore s where s.applicationId in "
      + "(select a.id from Application a "
      + " where a.generationId = :generationId and a.status <> :excluded)")
  List<EvaluationScore> findByGenerationIdExcludingStatus(
      @Param("generationId") Long generationId,
      @Param("excluded") ApplicationStatus excluded);

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
