package com.getit.domain.qna.repository;

import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, Long> {

  @Query("""
      select q from Question q
      where (:status is null or q.status = :status)
        and (:siteOnly = false or q.lectureId is null)
        and (:lectureId is null or q.lectureId = :lectureId)
        and (:keyword is null or q.content like :keyword or q.authorId in :authorIds)
      order by q.createdAt desc, q.id desc
      """)
  Page<Question> search(
      @Param("status") QnaStatus status,
      @Param("siteOnly") boolean siteOnly,
      @Param("lectureId") Long lectureId,
      @Param("keyword") String keyword,
      @Param("authorIds") List<Long> authorIds,
      Pageable pageable
  );

  List<Question> findByLectureIdAndAuthorIdOrderByCreatedAtDescIdDesc(Long lectureId, long authorId);

  long countByStatus(QnaStatus status);

  List<Question> findByStatusOrderByCreatedAtDescIdDesc(QnaStatus status, Pageable pageable);
}
