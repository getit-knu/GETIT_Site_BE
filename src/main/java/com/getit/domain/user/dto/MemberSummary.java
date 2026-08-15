package com.getit.domain.user.dto;

import com.getit.domain.user.entity.User;

/**
 * 다른 도메인에 노출하는 부원 요약 정보. (작업 분할 계획 4.2, 이슈 #30)
 *
 * <p>{@code lecture} 의 8.6 제출 현황 화면(모집단 조회)에서 쓴다.
 *
 * <p>{@code groupId} 는 아직 넣지 않았다. 조(Group) 관리(9.6~9.11)가 구현되기 전이라 소스가 없다.
 * 필요해지면 그때 필드를 추가한다.
 */
public record MemberSummary(
    Long userId,
    String userName,
    String major
) {

  public static MemberSummary from(User user) {
    return new MemberSummary(user.getId(), user.getName(), user.getMajor());
  }
}
