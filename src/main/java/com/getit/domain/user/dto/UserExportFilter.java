package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;

/**
 * 9.5 엑셀 다운로드 필터. 9.1 목록 조회와 동일한 필터 조합을 쓴다.
 *
 * <p>{@code keyword} 와 {@code groupId} 가 둘 다 {@code String} 이라 서비스 메서드가 네 인자를
 * 그대로 받으면 호출부에서 순서가 바뀌어도 컴파일 에러 없이 통과한다. 필터 값을 record 로 묶어서
 * 인자 순서 실수를 컴파일 타임에 막는다 (PR #71 Copilot 리뷰 지적).
 */
public record UserExportFilter(
    String keyword,
    Role role,
    String groupId,
    Integer generationNo
) {

}
