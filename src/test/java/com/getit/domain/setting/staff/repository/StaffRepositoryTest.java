package com.getit.domain.setting.staff.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class StaffRepositoryTest {

  @Autowired
  private StaffRepository staffRepository;

  private Staff staff(Integer generationNo, StaffSection section, Integer order, String name) {
    return Staff.create(generationNo, order, section, "역할", name, "학과", null, null, null);
  }

  @Test
  @DisplayName("기수의 운영진을 section → order 순으로 조회한다")
  void findsByGenerationNoOrderBySectionAscOrderAscIdAsc() {
    staffRepository.save(staff(9, StaffSection.SW, 2, "이영희"));
    staffRepository.save(staff(9, StaffSection.EXECUTIVE, 1, "김철수"));
    staffRepository.save(staff(9, StaffSection.SW, 1, "홍길동"));
    staffRepository.save(staff(8, StaffSection.SW, 1, "지난 기수"));

    assertThat(staffRepository.findByGenerationNoOrderBySectionAscOrderAscIdAsc(9))
        .extracting(Staff::getName)
        .containsExactly("김철수", "홍길동", "이영희");
  }

  @Test
  @DisplayName("order 가 같으면 id 오름차순으로 정렬한다")
  void tieBreaksById() {
    Staff first = staffRepository.save(staff(9, StaffSection.SW, 1, "A"));
    Staff second = staffRepository.save(staff(9, StaffSection.SW, 1, "B"));

    assertThat(staffRepository.findByGenerationNoOrderBySectionAscOrderAscIdAsc(9))
        .extracting(Staff::getId)
        .containsExactly(first.getId(), second.getId());
  }

  @Test
  @DisplayName("기수·section 이 둘 다 일치하는 운영진만 조회한다")
  void findsByGenerationNoAndSection() {
    staffRepository.save(staff(9, StaffSection.SW, 1, "홍길동"));
    staffRepository.save(staff(9, StaffSection.EXECUTIVE, 1, "김철수"));

    assertThat(staffRepository.findByGenerationNoAndSection(9, StaffSection.SW))
        .extracting(Staff::getName)
        .containsExactly("홍길동");
  }

  @Test
  @DisplayName("id 와 소속 기수가 둘 다 일치할 때만 조회한다")
  void findsByIdAndGenerationNoOnlyWhenBothMatch() {
    Staff saved = staffRepository.save(staff(9, StaffSection.SW, 1, "홍길동"));

    assertThat(staffRepository.findByIdAndGenerationNo(saved.getId(), 9)).isPresent();
    assertThat(staffRepository.findByIdAndGenerationNo(saved.getId(), 8)).isEmpty();
  }
}
