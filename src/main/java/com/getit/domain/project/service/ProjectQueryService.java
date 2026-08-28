package com.getit.domain.project.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 공개 사이트(2.4 쇼케이스 · 2.1 홈)가 소비하는 프로젝트 조회 계약. 순수 project 데이터만 반환한다 —
 * thumbnailUrl(fileId 해석)은 소비자가 FileQueryService 로 채운다.
 */
public interface ProjectQueryService {

  Page<ProjectView> findShowcase(String semester, Pageable pageable);

  List<String> findDistinctSemesters();

  List<ProjectView> findFeatured();
}
