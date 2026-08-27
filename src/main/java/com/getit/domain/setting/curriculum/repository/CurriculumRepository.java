package com.getit.domain.setting.curriculum.repository;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

  /** 10.10 목록. 카드가 노출되는 순서 그대로 반환한다. */
  List<Curriculum> findByGenerationIdOrderByOrderAsc(Long generationId);

  /**
   * id 와 소속 기수를 함께 확인한다. (10.12 · 10.13) id 만으로 찾으면 비활성(과거) 기수의
   * 커리큘럼도 수정 · 삭제할 수 있다 ({@code EvaluationCriterionRepository
   * .findByIdAndGenerationId} 와 동일한 이유 · 패턴).
   */
  Optional<Curriculum> findByIdAndGenerationId(Long id, Long generationId);
}
