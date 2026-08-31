package com.getit.domain.project.repository;

import com.getit.domain.project.entity.Project;
import com.getit.domain.project.entity.ProjectStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  /**
   * semester 와 status 둘 다 선택 필터다.
   *
   * <p>공개 쇼케이스는 {@code APPROVED} 만 넘기고, 어드민 목록은 승인 대기 중인 것도 봐야 하므로
   * {@code null} 을 넘겨 전부 읽는다 (이슈 #148).
   */
  @Query("""
      select p from Project p
      where (:semester is null or p.semester = :semester)
        and (:status is null or p.status = :status)
      order by p.order asc, p.id asc
      """)
  Page<Project> searchBySemester(
      @Param("semester") String semester,
      @Param("status") ProjectStatus status,
      Pageable pageable);

  @Query("select distinct p.semester from Project p where p.status = :status order by p.semester desc")
  List<String> findDistinctSemestersByStatus(@Param("status") ProjectStatus status);

  List<Project> findByIsFeaturedTrueAndStatusOrderByOrderAscIdAsc(ProjectStatus status);

  /**
   * 우리 조가 낸 프로젝트. 최신순. (이슈 #190)
   *
   * <p>조 id 로 찾는다. 조 이름은 기수 안에서만 유일하고 바뀔 수도 있어 소유 판정에 쓸 수
   * 없다 (PR #197 리뷰). 개인이 아니라 조의 것이라 낸 사람이 아니어도 같은 조원이면 보인다.
   */
  List<Project> findByGroupIdOrderByIdDesc(Long groupId);

  @Query("select coalesce(max(p.order), 0) from Project p")
  int findMaxOrder();
}
