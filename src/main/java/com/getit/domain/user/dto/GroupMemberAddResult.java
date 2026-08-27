package com.getit.domain.user.dto;

/** 조원 추가 결과. (API 명세서 9.10) */
public record GroupMemberAddResult(
    Long groupId,
    int addedCount,
    int memberCount
) { }
