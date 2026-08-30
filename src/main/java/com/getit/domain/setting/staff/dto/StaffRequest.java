package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.StaffSection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 운영진 추가 · 수정 요청. (API 명세서 10.21)
 *
 * <p>{@code order} 는 받지 않는다 — 생성 시 서버가 자동으로 다음 순번을 매기고, 순서 변경은
 * 10.22 전용 엔드포인트로만 한다.
 *
 * <p>{@code generationNo} 는 현재 활성 기수와 일치해야 한다.
 *
 * <p>문자열 필드는 엔티티 컬럼 길이(name·staffRole 50, department 100, introduction 255)에
 * 맞춰 {@code @Size} 로 미리 막는다 — 검증 없이 그대로 저장을 시도하면 DB 제약에서 500 으로
 * 실패했다(PR #82 Copilot 리뷰 지적).
 */
public record StaffRequest(
    Long userId,
    @NotBlank @Size(max = 50) String name,
    @NotBlank @Size(max = 50) String staffRole,
    @NotNull StaffSection section,
    @NotBlank @Size(max = 100) String department,
    @Size(max = 255) String introduction,
    // http · https 만 받는다. 공개 화면이 이 값을 그대로 href 에 넣으므로,
    // javascript: 같은 스킴을 허용하면 운영진 카드가 XSS · 피싱 통로가 된다
    // (PR #158 Copilot 리뷰 지적). null 은 @Pattern 이 통과시킨다.
    @Size(max = 512) @Pattern(regexp = URL_SCHEME, message = "http 또는 https 주소여야 합니다.")
    String githubUrl,
    @Size(max = 512) @Pattern(regexp = URL_SCHEME, message = "http 또는 https 주소여야 합니다.")
    String instagramUrl,
    Long fileId,
    @NotNull Integer generationNo
) {

  /** 스킴을 http · https 로 제한한다. */
  private static final String URL_SCHEME = "^https?://\\S+$";

  public StaffCommand toCommand() {
    return new StaffCommand(
        section, staffRole, name, department, introduction, githubUrl, instagramUrl, userId, fileId);
  }
}
