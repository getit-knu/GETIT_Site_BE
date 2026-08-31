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
class CollegeMajorControllerTest {

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

  /**
   * 명세서 2.7 의 경로. 지금까지 404 였다. (이슈 #193)
   *
   * <p>지원서 폼의 단과대 · 학과 셀렉트가 이 경로를 쓴다. FE 가 명세를 보고 붙이면 404 가
   * 나던 상태였다.
   */
  @Nested
  @DisplayName("GET " + COLLEGES_PATH + "/{collegeId}/majors")
  class GetMajorsOfCollege {

    @Test
    @DisplayName("그 단과대의 전공만 반환한다")
    void returnsMajorsOfCollege() throws Exception {
      College engineering = collegeRepository.save(College.create("IT대학"));
      majorRepository.save(Major.create(business.getId(), "경영학과"));
      majorRepository.save(Major.create(engineering.getId(), "컴퓨터학부"));

      mockMvc.perform(get(COLLEGES_PATH + "/" + engineering.getId() + "/majors"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.length()").value(1))
          .andExpect(jsonPath("$.data[0].name").value("컴퓨터학부"));
    }

    @Test
    @DisplayName("인증 없이 열린다")
    void openWithoutAuth() throws Exception {
      mockMvc.perform(get(COLLEGES_PATH + "/" + business.getId() + "/majors"))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("없는 단과대 id 면 빈 목록이다")
    void returnsEmptyForUnknownCollege() throws Exception {
      // 화면 입장에서 "그런 단과대가 없다" 와 "그 단과대에 학과가 없다" 는
      // 똑같이 고를 것이 없는 상태다. 404 로 나누지 않는다.
      mockMvc.perform(get(COLLEGES_PATH + "/999999/majors"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("쿼리 파라미터 방식과 같은 결과다")
    void matchesQueryParamVariant() throws Exception {
      majorRepository.save(Major.create(business.getId(), "경영학과"));

      String nested = mockMvc.perform(get(COLLEGES_PATH + "/" + business.getId() + "/majors"))
          .andReturn().getResponse().getContentAsString();
      String query = mockMvc.perform(
              get(MAJORS_PATH).param("collegeId", business.getId().toString()))
          .andReturn().getResponse().getContentAsString();

      // 같은 서비스 메서드를 쓰므로 동작이 갈라지면 안 된다.
      org.assertj.core.api.Assertions.assertThat(nested).isEqualTo(query);
    }
  }
}
