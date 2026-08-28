package com.getit.domain.setting.faq.repository;

import com.getit.domain.setting.faq.entity.Faq;
import jakarta.persistence.LockModeType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FaqRepository extends JpaRepository<Faq, Long> {

  /**
   * 10.18 목록. 노출 순서 그대로 반환한다. faq_order 만으로 정렬하면 같은 값을 가진 행이 있을 때
   * 순서가 실행 계획에 따라 달라질 수 있어 id 를 tie-breaker 로 더한다
   * (PR #78 Copilot 리뷰 지적, {@code CurriculumRepository} 와 동일한 이유).
   */
  List<Faq> findAllByOrderByOrderAscIdAsc();

  /**
   * 생성 · 순서 이동 · 삭제에서 목록을 읽을 때 쓴다. {@code PESSIMISTIC_WRITE} 로 전체 행을 잠근다 —
   * 목록 크기(또는 순번 위치)를 읽고 그 값을 근거로 새 순번을 저장하는 과정이 원자적이지 않으면,
   * 동시에 들어온 두 요청이 같은 순번을 계산해 1..N 불변식을 깬다(PR #101 Copilot 리뷰 지적,
   * {@code StaffRepository.findByGenerationNoAndSection} 와 동일한 이유).
   *
   * <p>다만 테이블이 비어 있는 최초 상태의 경합은 잠글 행 자체가 없어 막지 못한다 — 관리자 전용 ·
   * 저빈도 쓰기라 전용 잠금 행을 두기엔 비용 대비 효과가 낮다고 판단해 보강하지 않았다
   * ({@code StaffRepository} 와 동일한 선택).
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from Faq f order by f.order asc, f.id asc")
  List<Faq> findAllForUpdate();
}
