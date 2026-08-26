package com.getit.domain.user.dto;

import java.util.List;

/**
 * 조 관리 보드 전체 응답. (API 명세서 9.6)
 *
 * <p>{@code unassigned} 는 조 배정 UI 에서 드래그 소스로 쓰인다.
 */
public record GroupBoardResult(
    Integer generationNo,
    List<GroupWithMembersResult> groups,
    List<GroupMemberResult> unassigned
) { }
