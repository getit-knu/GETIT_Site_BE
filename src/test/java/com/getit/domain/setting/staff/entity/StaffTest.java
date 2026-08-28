package com.getit.domain.setting.staff.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StaffTest {

  @Test
  @DisplayName("생성한다")
  void creates() {
    Staff staff = Staff.create(
        9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21",
        "프론트엔드를 담당하고 있습니다.", 3L, 701L);

    assertThat(staff.getGenerationNo()).isEqualTo(9);
    assertThat(staff.getOrder()).isEqualTo(1);
    assertThat(staff.getSection()).isEqualTo(StaffSection.SW);
    assertThat(staff.getStaffRole()).isEqualTo("SW 운영진");
    assertThat(staff.getName()).isEqualTo("홍길동");
    assertThat(staff.getDepartment()).isEqualTo("컴퓨터공학과 21");
    assertThat(staff.getIntroduction()).isEqualTo("프론트엔드를 담당하고 있습니다.");
    assertThat(staff.getUserId()).isEqualTo(3L);
    assertThat(staff.getFileId()).isEqualTo(701L);
  }

  @Test
  @DisplayName("userId · fileId 없이도 생성된다 (표시 전용 프로필)")
  void createsWithoutUserOrFile() {
    Staff staff = Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null);

    assertThat(staff.getUserId()).isNull();
    assertThat(staff.getFileId()).isNull();
    assertThat(staff.getIntroduction()).isNull();
  }

  @Test
  @DisplayName("order 를 바꾸지 않고 나머지 정보를 수정한다")
  void updatesWithoutChangingOrder() {
    Staff staff = Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null);

    staff.update(StaffSection.SW, "부회장", "이영희", "전자공학과 19", "안녕하세요", 5L, 702L);

    assertThat(staff.getOrder()).isEqualTo(1);
    assertThat(staff.getStaffRole()).isEqualTo("부회장");
    assertThat(staff.getName()).isEqualTo("이영희");
    assertThat(staff.getDepartment()).isEqualTo("전자공학과 19");
    assertThat(staff.getIntroduction()).isEqualTo("안녕하세요");
    assertThat(staff.getUserId()).isEqualTo(5L);
    assertThat(staff.getFileId()).isEqualTo(702L);
  }

  @Test
  @DisplayName("순서를 변경한다")
  void updatesOrder() {
    Staff staff = Staff.create(9, 1, StaffSection.SW, "SW 운영진", "홍길동", "컴퓨터공학과 21", null, null, null);

    staff.updateOrder(3);

    assertThat(staff.getOrder()).isEqualTo(3);
  }
}
