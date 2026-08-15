package com.getit.domain.recruitment.repository;

import com.getit.domain.recruitment.entity.ApplicationQuestion;
import java.util.List;
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
}
