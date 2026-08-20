package com.getit.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.entity.Major;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class MajorRepositoryTest {

  @Autowired
  private MajorRepository majorRepository;

  @Test
  @DisplayName("전체를 등록 순서(id 오름차순)로 조회한다")
  void findsAllOrderedById() {
    Major business = majorRepository.save(Major.create(1L, "경영학과"));
    Major businessInfo = majorRepository.save(Major.create(1L, "경영정보학과"));
    majorRepository.save(Major.create(2L, "컴퓨터공학과"));

    assertThat(majorRepository.findAllByOrderByIdAsc())
        .extracting(Major::getId)
        .contains(business.getId(), businessInfo.getId());
  }

  @Test
  @DisplayName("collegeId 로 필터링해서 조회한다")
  void findsByCollegeId() {
    majorRepository.save(Major.create(1L, "경영학과"));
    majorRepository.save(Major.create(1L, "경영정보학과"));
    majorRepository.save(Major.create(2L, "컴퓨터공학과"));

    assertThat(majorRepository.findByCollegeIdOrderByIdAsc(1L))
        .extracting(Major::getName)
        .containsExactly("경영학과", "경영정보학과");
  }
}
