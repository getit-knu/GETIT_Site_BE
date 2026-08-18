package com.getit.domain.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.entity.College;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class CollegeRepositoryTest {

  @Autowired
  private CollegeRepository collegeRepository;

  @Test
  @DisplayName("등록 순서(id 오름차순)로 조회한다")
  void findsAllOrderedById() {
    College business = collegeRepository.save(College.create("경영대학"));
    College engineering = collegeRepository.save(College.create("공과대학"));

    assertThat(collegeRepository.findAllByOrderByIdAsc())
        .extracting(College::getId)
        .containsExactly(business.getId(), engineering.getId());
  }
}
