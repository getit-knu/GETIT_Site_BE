package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Group;

/** 사용자 목록(9.1)에 포함되는 조 요약. {@link GroupResult} 보다 가벼운(id · name 만) 표현이다. */
public record GroupSummary(
    Long id,
    String name
) {

  public static GroupSummary from(Group group) {
    return new GroupSummary(group.getId(), group.getName());
  }
}
