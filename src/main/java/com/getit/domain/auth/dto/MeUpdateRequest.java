package com.getit.domain.auth.dto;

import com.getit.domain.user.dto.ProfileEditCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 내 프로필 수정 요청. (이슈 #147)
 *
 * <p>학과 · 학번 · 기수 · 권한 · 상태는 받지 않는다. 지원서와 어드민 승격으로 정해지는
 * 값이라 본인이 바꾸면 심사 결과와 어긋난다.
 *
 * @param profileFileId 새 프로필 사진의 파일 id. 사진을 바꾸지 않을 때는 비워 보낸다.
 *                      비워 보내면 지금 사진이 그대로 남는다 — 사진이 지워지지 않는다
 */
public record MeUpdateRequest(
    @NotBlank @Size(max = 50) String name,
    @Size(max = 20) String phoneNumber,
    @Positive Long profileFileId,
    @Positive Long collegeId,
    @Positive Long majorId
) {

  public ProfileEditCommand toCommand() {
    return new ProfileEditCommand(name, phoneNumber, profileFileId, collegeId, majorId);
  }
}
