package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import jakarta.validation.constraints.Size;

/**
 * 사용자 권한 · 그룹 · 기수 변경 요청. (API 명세서 9.2)
 *
 * <p>변경할 필드만 보낸다(partial update) — 필드가 모두 nullable 이고, null 인 필드는
 * 그대로 둔다. 그래서 다른 요청 DTO 와 달리 {@code @NotNull} 을 걸지 않는다.
 *
 * @param groupId 배정할 조. {@code null} 은 "조를 건드리지 않는다" 는 뜻이지 해제가 아니다
 * @param unassignGroup 조 배정을 푼다. {@code groupId} 의 {@code null} 이 이미 "안 건드림" 으로
 *                      쓰이고 있어, 해제를 표현할 자리가 없었다 (이슈 #174). 0 같은 값을
 *                      해제로 약속하는 방법도 있지만, 유효한 id 처럼 생긴 값에 다른 뜻을
 *                      숨기면 다음 사람이 반드시 걸린다. 뜻이 다르므로 자리를 따로 둔다
 */
public record UserUpdateRequest(
    Role role,
    Long groupId,
    Integer generationNo,
    Boolean unassignGroup,
    @Size(max = 50) String college,
    @Size(max = 50) String major
) {

  public boolean unassignGroupOrDefault() {
    return Boolean.TRUE.equals(unassignGroup);
  }
}
