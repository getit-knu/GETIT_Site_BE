package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Group;

/** 조 생성 · 수정 결과. (API 명세서 9.7 · 9.8) */
public record GroupResult(
    Long id,
    String name,
    Integer generationNo,
    int memberCount
) {

  public static GroupResult of(Group group, Integer generationNo, int memberCount) {
    return new GroupResult(group.getId(), group.getName(), generationNo, memberCount);
  }
}
