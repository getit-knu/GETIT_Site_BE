package com.getit.domain.user.service;

import com.getit.domain.user.dto.GroupWithMembersResult;
import java.util.Optional;

/**
 * 부원이 자기 조를 읽는 계약. (이슈 #148)
 *
 * <p>project 가 "자기 조 명의로 등록"할 때 조 이름을 여기서 얻는다. 다른 도메인은
 * UserRepository 를 직접 참조하지 않는다 (작업 분할 계획 4.2).
 *
 * @see UserAccountService
 */
public interface MemberGroupService {

  /** 아직 조에 배정되지 않았으면 비어 있다. 배정 전은 오류가 아니라 정상 상태다. */
  Optional<GroupWithMembersResult> findMyGroup(Long userId);
}
