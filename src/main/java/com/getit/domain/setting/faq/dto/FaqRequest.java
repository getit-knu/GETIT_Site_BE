package com.getit.domain.setting.faq.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * FAQ 추가 · 수정 요청. (API 명세서 10.19)
 *
 * <p>{@code order} 는 생략 가능하다 — null 이면 생성 시 맨 뒤(MAX+1), 수정 시 현재 순서를 유지한다.
 * 값이 있으면 {@code FaqAdminService} 가 [1, N] 로 clamp 하고 그 사이 항목을 밀거나 당긴다.
 *
 * <p>{@code question} · {@code answer} 는 엔티티 컬럼 길이(255 · 2000)에 맞춰 {@code @Size} 로 미리
 * 막는다 — 검증 없이 저장하면 DB 제약에서 500 으로 실패한다(PR #78 Copilot 리뷰 지적).
 */
public record FaqRequest(
    @NotBlank @Size(max = 255) String question,
    @NotBlank @Size(max = 2000) String answer,
    @NotNull Boolean isVisible,
    @Min(1) Integer order
) {

  public FaqCommand toCommand() {
    return new FaqCommand(question, answer, isVisible);
  }
}
