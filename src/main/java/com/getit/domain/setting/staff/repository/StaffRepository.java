package com.getit.domain.setting.staff.repository;

import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, Long> {

  /** 10.21 목록. section 별로 묶여 보이도록 section → order 순으로 반환한다. */
  List<Staff> findByGenerationNoOrderBySectionAscOrderAsc(Integer generationNo);

  /** 10.22 순서 변경 대상. section 안에서만 순서를 재부여하므로 그 section 소속만 가져온다. */
  List<Staff> findByGenerationNoAndSection(Integer generationNo, StaffSection section);

  /**
   * id 와 소속 기수를 함께 확인한다. (10.21 수정 · 삭제) id 만으로 찾으면 비활성(과거) 기수의
   * 운영진도 수정 · 삭제할 수 있다 ({@code EvaluationCriterionRepository.findByIdAndGenerationId}
   * 와 동일한 이유 · 패턴).
   */
  Optional<Staff> findByIdAndGenerationNo(Long id, Integer generationNo);

  /** 새 운영진의 order 자동 부여(count + 1)에 쓴다. (10.21 생성, section 이동) */
  long countByGenerationNoAndSection(Integer generationNo, StaffSection section);
}
