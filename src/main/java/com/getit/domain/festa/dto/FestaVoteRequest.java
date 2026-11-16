package com.getit.domain.festa.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 투표하거나 투표한 팀을 바꾼다. 번호 규칙은 포트폴리오 저장과 같다. (이슈 #232) */
public record FestaVoteRequest(
    @NotNull Long projectId,
    @Size(max = 20) String phoneNumber,
    Boolean privacyConsent
) { }
