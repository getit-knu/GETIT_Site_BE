package com.getit.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.6 GET /api/public/colleges · 2.7 GET /api/public/majors */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollegeControllerTest {

  private static final String COLLEGES_PATH = "/api/public/colleges";
  private static final String MAJORS_PATH = "/api/public/majors";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private MajorRepository majorRepository;

  private College business;

  @BeforeEach
  void setUpColleges() {
    business = collegeRepository.save(College.create("경영대학"));
    collegeRepository.save(College.create("공과대학"));
  }

  @Nested
  @DisplayName("GET " + COLLEGES_PATH)
  class GetColleges {

    @Test
    @DisplayName("인증 없이 단과대학 목록을 반환한다")
    void returnsCollegesWithoutAuth() throws Exception {
      mockMvc.perform(get(COLLEGES_PATH))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].name").value("경영대학"))
          .andExpect(jsonPath("$.data[1].name").value("공과대학"));
    }
  }

  @Nested
  @DisplayName("GET " + MAJORS_PATH)
  class GetMajors {

    @Test
    @DisplayName("collegeId 로 필터링해서 전공 목록을 반환한다")
    void returnsMajorsFilteredByCollege() throws Exception {
      majorRepository.save(Major.create(business.getId(), "경영학과"));
      majorRepository.save(Major.create(business.getId(), "경영정보학과"));

      mockMvc.perform(get(MAJORS_PATH).param("collegeId", business.getId().toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].name").value("경영학과"))
          .andExpect(jsonPath("$.data[1].name").value("경영정보학과"));
    }

    @Test
    @DisplayName("collegeId 가 없으면 전체 전공을 반환한다")
    void returnsAllMajorsWithoutCollegeId() throws Exception {
      majorRepository.save(Major.create(business.getId(), "경영학과"));

      mockMvc.perform(get(MAJORS_PATH))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isArray());
    }
  }
}
