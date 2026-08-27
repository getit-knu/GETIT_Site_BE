package com.getit.domain.user.repository;

import com.getit.domain.user.entity.Group;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

  /** 기수별 조 목록. 생성 순서(id 오름차순)로 정렬한다. (9.6) */
  List<Group> findByGenerationIdOrderByIdAsc(Long generationId);

  /** 같은 기수 안에서 이름 중복 여부. (9.7) */
  boolean existsByGenerationIdAndName(Long generationId, String name);

  /** 자기 자신을 제외하고 같은 기수 안에서 이름 중복 여부. (9.8) */
  boolean existsByGenerationIdAndNameAndIdNot(Long generationId, String name, Long id);

  /**
   * id 와 소속 기수를 함께 확인한다. (9.8~9.11)
   *
   * <p>id 만으로 찾으면 활성 기수가 아닌(과거·미래) 조도 이름 변경·삭제·조원 변경이 가능해진다
   * (PR #60 Copilot 리뷰 지적, 팀 논의 후 반영 — {@code EvaluationCriterionRepository
   * .findByIdAndGenerationId} 와 동일한 이유·패턴).
   */
  Optional<Group> findByIdAndGenerationId(Long id, Long generationId);
}
