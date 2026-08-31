package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;

/**
 * 사용자 권한 · 조 · 기수 변경 명령. (API 명세서 9.2)
 *
 * <p>변경 필드를 한 덩어리로 묶는다. 풀어서 넘기면 인자가 여섯 개가 되고 그중 둘이
 * {@code Long} 으로 붙어 있어({@code targetUserId} · {@code currentUserId}) 순서를 잘못 넘기는
 * 실수를 컴파일러가 잡지 못한다({@link com.getit.domain.recruitment.dto.ScheduleUpdateCommand}
 * 과 같은 이유, PR #181 리뷰 지적).
 *
 * @param groupId 배정할 조. {@code null} 은 "조를 건드리지 않는다" 는 뜻이지 해제가 아니다
 * @param unassignGroup 조 배정을 푼다. {@code groupId} 의 {@code null} 이 이미 "안 건드림" 으로
 *                      쓰이고 있어 해제를 표현할 자리가 없었다 (이슈 #174)
 * @param college 단과대 이름. 값이 채워지는 정상 경로는 승격(9.4)이지만, 지원서에 단과대 id 가
 *                담기지 않던 동안 승격된 부원은 비어 있고 그 지원서에는 되살릴 원본도 없다.
 *                손으로 채울 자리가 필요하다 (이슈 #192)
 * @param major 학과 이름. {@code college} 와 같다
 */
public record UserUpdateCommand(
    Role role,
    Long groupId,
    Integer generationNo,
    boolean unassignGroup,
    String college,
    String major
) { }
