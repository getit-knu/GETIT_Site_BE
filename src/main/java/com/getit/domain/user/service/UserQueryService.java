package com.getit.domain.user.service;

import com.getit.domain.user.dto.MemberSummary;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * lecture · dashboard 가 소비하는 부원 조회 계약. (작업 분할 계획 4.2, 이슈 #30 · D5)
 *
 * <p>{@code UserRepository} 직접 참조를 대체한다. 8.6 제출 현황 화면이
 * "user LEFT JOIN submission"으로 모집단을 구해야 하는데, 그 모집단(활성 부원 목록)을 여기서 내려준다.
 *
 * <p>{@code UserAccountService}(auth 소비)와는 소비자·용도가 달라 별도 인터페이스로 둔다.
 */
public interface UserQueryService {

  /** 특정 기수의 활성 부원(role=MEMBER, status=ACTIVE) 목록. */
  List<MemberSummary> findActiveMembers(Integer generationNo);

  /** 전체 활성 부원 수 — 기수 무관. (D5.1 summary.memberCount) */
  long countActiveMembers();

  /** 특정 기수의 활성 부원 수. (D5.3 totalMemberCount · D5.5 totalCount) */
  long countActiveMembersInGeneration(Integer generationNo);

  /** id 로 이름을 일괄 조회한다. (D5.2 recent-questions 의 authorName) */
  Map<Long, String> findNamesByIds(Collection<Long> userIds);
}
