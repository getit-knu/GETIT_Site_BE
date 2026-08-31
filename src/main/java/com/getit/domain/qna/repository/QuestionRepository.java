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
        and (:keyword is null
             or q.content like :keyword
             or (:authorIds is not null and q.authorId in :authorIds))
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

  /**
   * 내가 쓴 질문 전체. 강의를 가로지른다. (이슈 #185)
   *
   * <p>강의별 조회(4.6)만 있으면 마이페이지의 "내 질문" 을 그리려고 프론트가 전체 강의를
   * 순회해 N 번 호출해야 한다. 페이징과 정렬도 클라이언트에서 억지로 맞추게 된다.
   *
   * <p>강의에 매이지 않은 질문({@code lectureId is null})도 함께 나온다 — 내가 쓴 질문이라는
   * 점은 같다.
   */
  @Query("select q from Question q "
      + "where q.authorId = :authorId "
      + "and (:status is null or q.status = :status) "
      + "order by q.createdAt desc, q.id desc")
  Page<Question> findMyQuestions(
      @Param("authorId") long authorId,
      @Param("status") QnaStatus status,
      Pageable pageable);

  long countByStatus(QnaStatus status);

  List<Question> findByStatusOrderByCreatedAtDescIdDesc(QnaStatus status, Pageable pageable);
}
