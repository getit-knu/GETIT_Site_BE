package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.EvaluationCriterion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationCriterionRepository extends JpaRepository<EvaluationCriterion, Long> {

  /**
   * 기수별 순서(order) 오름차순 조회. (6.8)
   *
   * <p>derived query 이름(...OrderByOrderAsc)이 order 라는 필드명과 겹쳐 헷갈리므로 명시적으로 쓴다.
   */
  @Query("select c from EvaluationCriterion c where c.generationId = :generationId order by c.order asc")
  List<EvaluationCriterion> findByGenerationId(@Param("generationId") Long generationId);

  /**
   * id 와 소속 기수를 함께 확인한다. (6.10 · 6.11)
   *
   * <p>활성 기수가 아닌 기준(이미 평가가 매겨진 기수 포함)은 수정 · 삭제 대상에서 제외해야
   * 기존 평가 점수가 orphan 상태가 되는 것을 막을 수 있다 ({@code ApplicationQuestion} 리뷰와 동일 이유).
   */
  Optional<EvaluationCriterion> findByIdAndGenerationId(Long id, Long generationId);
}
