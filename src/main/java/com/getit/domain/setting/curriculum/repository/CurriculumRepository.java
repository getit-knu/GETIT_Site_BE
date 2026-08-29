package com.getit.domain.setting.curriculum.repository;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurriculumRepository extends JpaRepository<Curriculum, Long> {

  /**
   * 10.10 목록. 카드가 노출되는 순서 그대로 반환한다. {@code order} 만으로 정렬하면 같은 값을
   * 가진 행이 있을 때(있어서는 안 되지만) 순서가 실행 계획에 따라 달라질 수 있어 {@code id}를
   * tie-breaker 로 더한다 (PR #78 Copilot 리뷰 지적 — {@code LectureRepository.findAllByFilters}
   * 와 동일한 이유).
   */
  List<Curriculum> findByGenerationIdOrderByOrderAscIdAsc(Long generationId);

  /**
   * id 와 소속 기수를 함께 확인한다. (10.12 · 10.13) id 만으로 찾으면 비활성(과거) 기수의
   * 커리큘럼도 수정 · 삭제할 수 있다 ({@code EvaluationCriterionRepository
   * .findByIdAndGenerationId} 와 동일한 이유 · 패턴).
   */
  Optional<Curriculum> findByIdAndGenerationId(Long id, Long generationId);

  /** 10.20 일괄 저장에서 커리큘럼 목록을 통째 교체할 때. 동시 CRUD 와 직렬화(B 의 {@code TrackRepository.findAllForUpdate}와 동일 패턴). */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Curriculum c where c.generationId = :generationId order by c.order asc")
  List<Curriculum> findByGenerationIdForUpdate(@Param("generationId") long generationId);
}
