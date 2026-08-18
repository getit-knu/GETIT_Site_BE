package com.getit.domain.setting.category.service;

import java.util.Optional;

/**
 * 다른 도메인이 트랙·소분류를 조회할 때 거치는 계약. (이슈 #25)
 *
 * <p>{@code TrackRepository}/{@code SubCategoryRepository} 직접 참조를 대체한다.
 * 현재 소비자는 {@code lecture}(강의 생성 시 트랙·소분류 존재·소속 검증).
 */
public interface CategoryQueryService {

  /** 트랙 존재 여부. */
  boolean existsTrack(Long trackId);

  /** 소분류가 속한 트랙 id. 소분류가 없으면 empty. */
  Optional<Long> findTrackIdOfSubCategory(Long subCategoryId);
}
