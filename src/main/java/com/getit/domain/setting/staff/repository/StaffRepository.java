package com.getit.domain.setting.staff.repository;

import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface StaffRepository extends JpaRepository<Staff, Long> {

  /**
   * 10.21 목록. section 별로 묶여 보이도록 section → order 순으로 반환한다. {@code order} 만으로는
   * 같은 값을 가진 행이 있을 때(있어서는 안 되지만) 순서가 비결정적이라 {@code id} 를 tie-breaker
   * 로 더한다 (PR #82 Copilot 리뷰 지적 — {@code CurriculumRepository} 와 동일한 이유).
   */
  List<Staff> findByGenerationNoOrderBySectionAscOrderAscIdAsc(Integer generationNo);

  /**
   * 10.22 순서 변경 대상이면서, 새 운영진의 order 자동 부여(count + 1)에도 쓴다. (10.21 생성,
   * section 이동)
   *
   * <p>{@code PESSIMISTIC_WRITE} 로 잠근다 — 이 section 의 인원 수(count)를 읽고 그 값+1 을
   * 새 순번으로 저장하는 과정이 원자적이지 않으면, 동시에 들어온 두 생성(또는 section 이동)
   * 요청이 같은 순번을 계산해 중복 저장할 수 있다(PR #82 Copilot 리뷰 지적). 이 조회로 해당
   * section 의 기존 행을 먼저 잠그면 두 번째 트랜잭션은 첫 번째가 커밋할 때까지 블록되고,
   * 그 뒤에는 늘어난 인원 수를 보게 된다. 다만 해당 section 에 행이 하나도 없는 최초 상태의
   * 경합은 잠글 대상 자체가 없어 막지 못한다 — {@code GenerationAdminService} 의 활성화 로직과
   * 달리 예약 행을 두기엔 section 조합(기수 x 3개 section)이 많아 비용 대비 효과가 낮다고
   * 판단해 보강하지 않았다(PR 리뷰 포인트).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  List<Staff> findByGenerationNoAndSection(Integer generationNo, StaffSection section);

  /**
   * id 와 소속 기수를 함께 확인한다. (10.21 수정 · 삭제) id 만으로 찾으면 비활성(과거) 기수의
   * 운영진도 수정 · 삭제할 수 있다 ({@code EvaluationCriterionRepository.findByIdAndGenerationId}
   * 와 동일한 이유 · 패턴).
   */
  Optional<Staff> findByIdAndGenerationNo(Long id, Integer generationNo);
}
