package com.getit.domain.project.repository;

import com.getit.domain.project.entity.Project;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

  @Query("""
      select p from Project p
      where (:semester is null or p.semester = :semester)
      order by p.order asc, p.id asc
      """)
  Page<Project> searchBySemester(@Param("semester") String semester, Pageable pageable);

  @Query("select distinct p.semester from Project p order by p.semester desc")
  List<String> findDistinctSemesters();

  List<Project> findByIsFeaturedTrueOrderByOrderAscIdAsc();

  @Query("select coalesce(max(p.order), 0) from Project p")
  int findMaxOrder();
}
