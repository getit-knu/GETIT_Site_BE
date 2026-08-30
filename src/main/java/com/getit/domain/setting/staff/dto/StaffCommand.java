package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;

/**
 * 운영진 등록 · 수정에 들어가는 값 묶음.
 *
 * <p>{@code Staff.create} 에 String · Long 인자가 줄줄이 늘어서 있으면 순서를 바꿔도
 * 컴파일된다. 필드가 늘어날 때마다 호출부에 {@code null} 이 연쇄적으로 붙는 문제도 있다
 * (PR #158 Copilot 리뷰 지적). FAQ · 프로젝트가 쓰는 Command 방식에 맞춘다.
 *
 * <p>{@code generationNo} 와 {@code order} 는 서버가 정하므로 여기 넣지 않는다.
 */
public record StaffCommand(
    StaffSection section,
    String staffRole,
    String name,
    String department,
    String introduction,
    String githubUrl,
    String instagramUrl,
    Long userId,
    Long fileId
) { }
