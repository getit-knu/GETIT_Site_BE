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

  List<Faq> findByIsVisibleTrueOrderByOrderAscIdAsc();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from Faq f order by f.order asc, f.id asc")
  List<Faq> findAllForUpdate();
}
