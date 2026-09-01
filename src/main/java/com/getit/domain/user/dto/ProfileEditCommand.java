package com.getit.domain.user.dto;

/**
 * 본인이 직접 고치는 프로필 값. (이슈 #147)
 *
 * @param profileFileId 새 프로필 사진의 파일 id. {@code null} 이면 사진은 그대로 둔다
 * @param collegeId 단과대학 id. 이름이 아니라 id 로 받는다 — 자유 입력으로 열면
 *                  "컴퓨터학부" · "컴퓨터공학부" 가 섞인다 (이슈 #199)
 * @param majorId 학과 id. {@code collegeId} 와 <b>함께</b> 와야 한다. 학과는 단과대에 속하므로
 *                한쪽만 바꾸는 것은 뜻이 없고, 어긋난 조합이 저장될 수 있다
 */
public record ProfileEditCommand(
    String name, String phoneNumber, Long profileFileId, Long collegeId, Long majorId) {

  /** 소속을 바꾸려는 요청인지. 둘 다 비었으면 건드리지 않는다는 뜻이다. */
  public boolean changesAffiliation() {
    return collegeId != null || majorId != null;
  }
}
