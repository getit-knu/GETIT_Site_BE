package com.getit.domain.project.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 프로젝트 반려 요청. (이슈 #190)
 *
 * <p>사유를 필수로 받는다. 이 기능의 목적이 "부원이 왜 반려됐는지 알게 하는 것" 인데,
 * 선택으로 두면 대부분 비어 오고 부원은 지금과 똑같이 이유를 모른다. 그러면 같은 이유로
 * 다시 낸다 — 그게 이슈에 적힌 문제다.
 *
 * @param reason 컬럼이 varchar(500) 이다
 */
public record ProjectRejectRequest(@NotBlank @Size(max = 500) String reason) {
}
