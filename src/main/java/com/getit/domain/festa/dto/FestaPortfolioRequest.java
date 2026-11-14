package com.getit.domain.festa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 기존 투자를 이 내용으로 통째로 바꾼다. (이슈 #222)
 *
 * <p>{@code phoneNumber} 가 비어 있으면 이미 저장된 번호를 유지한다. 번호를 바꿀 때만 보낸다.
 * 첫 저장에는 번호와 동의가 필수다.
 */
public record FestaPortfolioRequest(
    @Size(max = 20) String phoneNumber,
    Boolean privacyConsent,
    @NotNull @Size(max = 100) List<@Valid @NotNull Investment> investments
) {

  public record Investment(@NotNull Long projectId, @NotNull Long amount) { }
}
