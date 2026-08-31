package com.getit.domain.setting.staff.dto;

import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;

/**
 * 운영진 조회 · 저장 결과. (API 명세서 10.20 · 10.21)
 *
 * @param fileId 프로필 사진의 파일 id. 사진이 없으면 {@code null}.
 *               수정(10.21)은 전체 교체라 {@code fileId} 를 비워 보내면 사진이 <b>지워진다.</b>
 *               그런데 응답에 이 값이 없어서, 사진을 그대로 두고 직책만 고치는 것이
 *               불가능했다 — 무엇을 다시 실어 보내야 하는지 알 방법이 없었다.
 *               {@code profileImageUrl} 은 저장소 주소라 여기서 id 를 역산할 수 없다 (이슈 #187)
 * @param profileImageUrl 지금 바로 열 수 있는 주소. 화면에 그릴 때 쓴다
 */
public record StaffResult(
    Long id,
    Long userId,
    String name,
    String staffRole,
    StaffSection section,
    String department,
    String introduction,
    String githubUrl,
    String instagramUrl,
    Long fileId,
    String profileImageUrl,
    Integer order,
    Integer generationNo
) {

  /** introduction 이 비어 있으면 기본 문구를 채운다. */
  private static final String DEFAULT_INTRODUCTION = "한줄 소개를 작성해주세요";

  public static StaffResult of(Staff staff, String profileImageUrl) {
    return new StaffResult(
        staff.getId(),
        staff.getUserId(),
        staff.getName(),
        staff.getStaffRole(),
        staff.getSection(),
        staff.getDepartment(),
        staff.getIntroduction() != null ? staff.getIntroduction() : DEFAULT_INTRODUCTION,
        staff.getGithubUrl(),
        staff.getInstagramUrl(),
        staff.getFileId(),
        profileImageUrl,
        staff.getOrder(),
        staff.getGenerationNo()
    );
  }
}
