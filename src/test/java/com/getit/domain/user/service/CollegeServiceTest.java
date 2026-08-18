package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.user.dto.CollegeResult;
import com.getit.domain.user.dto.MajorResult;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CollegeServiceTest {

  @Autowired
  private CollegeService collegeService;

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private MajorRepository majorRepository;

  private College business;
  private College engineering;

  @BeforeEach
  void setUpColleges() {
    business = collegeRepository.save(College.create("경영대학"));
    engineering = collegeRepository.save(College.create("공과대학"));
  }

  @Nested
  @DisplayName("getColleges")
  class GetColleges {

    @Test
    @DisplayName("등록 순서대로 전체 단과대학을 반환한다")
    void returnsAllColleges() {
      List<CollegeResult> results = collegeService.getColleges();

      assertThat(results).extracting(CollegeResult::name)
          .containsExactly("경영대학", "공과대학");
    }
  }

  @Nested
  @DisplayName("getMajors")
  class GetMajors {

    @Test
    @DisplayName("collegeId 를 지정하면 해당 단과대학 전공만 반환한다")
    void returnsMajorsFilteredByCollege() {
      majorRepository.save(Major.create(business.getId(), "경영학과"));
      majorRepository.save(Major.create(business.getId(), "경영정보학과"));
      majorRepository.save(Major.create(engineering.getId(), "컴퓨터공학과"));

      List<MajorResult> results = collegeService.getMajors(business.getId());

      assertThat(results).extracting(MajorResult::name)
          .containsExactly("경영학과", "경영정보학과");
    }

    @Test
    @DisplayName("collegeId 를 지정하지 않으면 전체 전공을 반환한다")
    void returnsAllMajorsWhenCollegeIdIsNull() {
      majorRepository.save(Major.create(business.getId(), "경영학과"));
      majorRepository.save(Major.create(engineering.getId(), "컴퓨터공학과"));

      List<MajorResult> results = collegeService.getMajors(null);

      assertThat(results).hasSize(2);
    }
  }
}
