package com.getit.domain.festa.dto;

import java.util.List;

/** 내 투표 현황. projectId 는 아직 투표하지 않았으면 null. (이슈 #232) */
public record FestaVoteResult(Long projectId, String maskedPhoneNumber, List<Long> ownProjectIds) { }
