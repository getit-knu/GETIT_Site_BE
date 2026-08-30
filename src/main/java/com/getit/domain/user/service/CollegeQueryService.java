package com.getit.domain.user.service;

import java.util.Collection;
import java.util.Map;

/**
 * 다른 도메인이 단과대학 이름을 얻을 때 쓰는 계약.
 *
 * <p>{@code College} 는 user 도메인 소유다. 지원자 목록처럼 바깥 도메인에서 이름이 필요하면
 * Repository 를 직접 참조하지 않고 이 계약을 거친다.
 *
 * <p>낱개 조회가 아니라 <b>한 번에 여러 건</b>을 받는다. 목록 화면은 페이지 단위로 나가는데
 * 행마다 조회하면 N+1 이 된다. {@link UserQueryService#findNamesByIds} 와 같은 방식이다.
 */
public interface CollegeQueryService {

  /**
   * @param collegeIds 찾을 단과대학 id. 비어 있으면 빈 Map
   * @return id → 이름. 없는 id 는 결과에 담기지 않는다
   */
  Map<Long, String> findNamesByIds(Collection<Long> collegeIds);
}
