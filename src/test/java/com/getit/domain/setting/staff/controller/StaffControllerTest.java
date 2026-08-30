package com.getit.domain.setting.staff.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.staff.dto.StaffOrderRequest;
import com.getit.domain.setting.staff.dto.StaffRequest;
import com.getit.domain.setting.staff.dto.StaffCommand;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.repository.StaffRepository;
import com.getit.domain.user.entity.Role;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 10.21~10.22 /api/admin/setting/staffs */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaffControllerTest {

  private static final String STAFFS_PATH = "/api/admin/setting/staffs";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private StaffRepository staffRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String requestJson(StaffSection section, String name, Integer generationNo) throws Exception {
    return objectMapper.writeValueAsString(
        new StaffRequest(null, name, "SW 운영진", section, "컴퓨터공학과 21", null,
            null, null, null, generationNo));
  }

  @Nested
  @DisplayName("SNS 링크 검증")
  class SnsLinkValidation {

    @Test
    @DisplayName("javascript: 스킴은 400 으로 막는다")
    void rejectsJavascriptScheme() throws Exception {
      // 공개 화면이 이 값을 href 에 그대로 넣는다. 허용하면 운영진 카드가 XSS 통로가 된다.
      String body = objectMapper.writeValueAsString(new StaffRequest(
          null, "홍길동", "SW 운영진", StaffSection.SW, "컴퓨터공학과 21", null,
          "javascript:alert(1)", null, null, 9));

      mockMvc.perform(post(STAFFS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("https 주소는 통과한다")
    void acceptsHttps() throws Exception {
      String body = objectMapper.writeValueAsString(new StaffRequest(
          null, "홍길동", "SW 운영진", StaffSection.SW, "컴퓨터공학과 21", null,
          "https://github.com/hong", null, null, 9));

      mockMvc.perform(post(STAFFS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isCreated());
    }
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(STAFFS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(STAFFS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + STAFFS_PATH)
  class GetStaffs {

    @Test
    @DisplayName("활성 기수의 운영진 목록을 반환한다")
    void returnsStaffs() throws Exception {
      staffRepository.save(
          Staff.create(9, 1, new StaffCommand(StaffSection.SW,"SW 운영진","홍길동","컴퓨터공학과 21",null,null,null,null,null)));

      mockMvc.perform(get(STAFFS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data[0].name").value("홍길동"))
          .andExpect(jsonPath("$.data[0].section").value("SW"));
    }
  }

  @Nested
  @DisplayName("POST " + STAFFS_PATH)
  class CreateStaff {

    @Test
    @DisplayName("운영진을 추가한다")
    void createsStaff() throws Exception {
      mockMvc.perform(post(STAFFS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(StaffSection.SW, "홍길동", activeGeneration.getGenerationNo())))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.name").value("홍길동"))
          .andExpect(jsonPath("$.data.order").value(1));
    }

    @Test
    @DisplayName("generationNo 가 활성 기수와 다르면 404 다")
    void returns404WhenGenerationMismatch() throws Exception {
      mockMvc.perform(post(STAFFS_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(StaffSection.SW, "홍길동", 999)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("GENERATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + STAFFS_PATH + "/{id}")
  class UpdateStaff {

    @Test
    @DisplayName("운영진을 수정한다")
    void updatesStaff() throws Exception {
      Staff staff = staffRepository.save(
          Staff.create(9, 1, new StaffCommand(StaffSection.SW,"SW 운영진","홍길동","컴퓨터공학과 21",null,null,null,null,null)));

      mockMvc.perform(put(STAFFS_PATH + "/" + staff.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(StaffSection.SW, "이영희", activeGeneration.getGenerationNo())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.name").value("이영희"));
    }

    @Test
    @DisplayName("없는 운영진이면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(put(STAFFS_PATH + "/999")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(requestJson(StaffSection.SW, "이영희", activeGeneration.getGenerationNo())))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("STAFF_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("DELETE " + STAFFS_PATH + "/{id}")
  class DeleteStaff {

    @Test
    @DisplayName("운영진을 삭제한다")
    void deletesStaff() throws Exception {
      Staff staff = staffRepository.save(
          Staff.create(9, 1, new StaffCommand(StaffSection.SW,"SW 운영진","홍길동","컴퓨터공학과 21",null,null,null,null,null)));

      mockMvc.perform(delete(STAFFS_PATH + "/" + staff.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNoContent());

      assertThat(staffRepository.findById(staff.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 기수의 운영진이면 404 다 (삭제되지 않는다)")
    void returns404WhenBelongsToOtherGeneration() throws Exception {
      Staff other = staffRepository.save(
          Staff.create(8, 1, new StaffCommand(StaffSection.SW,"SW 운영진","지난기수","컴퓨터공학과",null,null,null,null,null)));

      mockMvc.perform(delete(STAFFS_PATH + "/" + other.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("STAFF_NOT_FOUND"));

      assertThat(staffRepository.findById(other.getId())).isPresent();
    }
  }

  @Nested
  @DisplayName("PUT " + STAFFS_PATH + "/order")
  class ReorderStaffs {

    @Test
    @DisplayName("section 안에서 순서를 재부여한다")
    void reordersStaffs() throws Exception {
      Staff first = staffRepository.save(
          Staff.create(9, 1, new StaffCommand(StaffSection.SW,"SW 운영진","홍길동","컴퓨터공학과 21",null,null,null,null,null)));
      Staff second = staffRepository.save(
          Staff.create(9, 2, new StaffCommand(StaffSection.SW,"SW 운영진","이영희","전자공학과 19",null,null,null,null,null)));
      String body = objectMapper.writeValueAsString(
          new StaffOrderRequest(StaffSection.SW, List.of(second.getId(), first.getId())));

      mockMvc.perform(put(STAFFS_PATH + "/order")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isNoContent());

      // second 가 1번이 된 것만 확인하면, first 가 그대로 1번에 남아 order 가 중복돼도
      // 테스트를 통과한다 — first 가 2번으로 밀렸는지도 함께 확인해야 1..n 불변식이 실제로
      // 지켜지는지 검증된다 (PR #83 Copilot 리뷰 지적).
      assertThat(staffRepository.findById(second.getId()).orElseThrow().getOrder()).isEqualTo(1);
      assertThat(staffRepository.findById(first.getId()).orElseThrow().getOrder()).isEqualTo(2);
    }
  }
}
