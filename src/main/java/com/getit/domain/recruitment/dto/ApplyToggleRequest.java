package com.getit.domain.recruitment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 지원 접수 스위치. (이슈 #170)
 *
 * <p>{@code null} 을 허용하지 않는다. 여닫는 것은 사고가 났을 때 누르는 버튼이라,
 * 값이 빠졌을 때 한쪽으로 기본값을 정해 두면 의도하지 않은 방향으로 눌릴 수 있다.
 */
public record ApplyToggleRequest(@NotNull Boolean enabled) {
}
