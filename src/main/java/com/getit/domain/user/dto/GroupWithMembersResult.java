package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Group;
import java.util.List;

/** 조 하나 + 조원 목록. (API 명세서 9.6) */
public record GroupWithMembersResult(
    Long id,
    String name,
    int memberCount,
    List<GroupMemberResult> members
) {

  public static GroupWithMembersResult of(Group group, List<GroupMemberResult> members) {
    return new GroupWithMembersResult(group.getId(), group.getName(), members.size(), members);
  }
}
