package com.getit.domain.project.repository;

import com.getit.domain.project.entity.ProjectImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectImageRepository extends JpaRepository<ProjectImage, Long> {

  List<ProjectImage> findByProjectIdOrderByKindAscSortOrderAsc(Long projectId);

  /** 여러 프로젝트의 이미지를 한 번에 읽는다. 목록 화면에서 프로젝트 수만큼 쿼리가 나가지 않게 한다. */
  List<ProjectImage> findByProjectIdInOrderByProjectIdAscKindAscSortOrderAsc(Collection<Long> projectIds);
}
