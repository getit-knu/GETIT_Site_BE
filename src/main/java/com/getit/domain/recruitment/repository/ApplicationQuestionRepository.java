package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.ApplicationQuestion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationQuestionRepository extends JpaRepository<ApplicationQuestion, Long> {

  /**
   * 기수별 순서(order) 오름차순 조회. (6.3)
   *
   * <p>derived query 이름(...OrderByOrderAsc)이 order 라는 필드명과 겹쳐 헷갈리므로 명시적으로 쓴다.
   */
  @Query("select q from ApplicationQuestion q where q.generationId = :generationId order by q.order asc")
  List<ApplicationQuestion> findByGenerationId(@Param("generationId") Long generationId);

  /** 새 질문의 order(마지막 + 1)를 정하는 데 쓴다. (6.4) */
  long countByGenerationId(Long generationId);

  /**
   * id 와 소속 기수를 함께 확인한다. (6.5 · 6.6)
   *
   * <p>활성 기수가 아닌 질문(이미 지원서가 제출된 기수 포함)은 수정 · 삭제 대상에서 제외해야
   * 기존 답변이 orphan 상태가 되는 것을 막을 수 있다 (#33 리뷰).
   */
  Optional<ApplicationQuestion> findByIdAndGenerationId(Long id, Long generationId);
}
