package com.getit.domain.user.repository;

import com.getit.domain.user.entity.Group;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {

  /** 기수별 조 목록. 생성 순서(id 오름차순)로 정렬한다. (9.6) */
  List<Group> findByGenerationIdOrderByIdAsc(Long generationId);

  /** 같은 기수 안에서 이름 중복 여부. (9.7) */
  boolean existsByGenerationIdAndName(Long generationId, String name);

  /** 자기 자신을 제외하고 같은 기수 안에서 이름 중복 여부. (9.8) */
  boolean existsByGenerationIdAndNameAndIdNot(Long generationId, String name, Long id);
}
