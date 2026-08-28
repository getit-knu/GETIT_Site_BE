package com.getit.domain.setting.staff.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.setting.staff.entity.Staff;
import com.getit.domain.setting.staff.entity.StaffSection;
import com.getit.domain.setting.staff.repository.StaffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.3 GET /api/public/staffs */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaffPublicControllerTest {

  private static final String STAFFS_PATH = "/api/public/staffs";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private StaffRepository staffRepository;

  @Test
  @DisplayName("인증 없이 조회할 수 있고, section 별로 그룹핑해서 반환한다")
  void returnsStaffDirectoryWithoutAuthentication() throws Exception {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    staffRepository.save(
        Staff.create(9, 1, StaffSection.EXECUTIVE, "회장", "김철수", "경영학과 20", null, null, null));

    mockMvc.perform(get(STAFFS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sections[0].section").value("EXECUTIVE"))
        .andExpect(jsonPath("$.data.sections[0].sectionName").value("회장단"))
        .andExpect(jsonPath("$.data.sections[0].staffs[0].name").value("김철수"))
        .andExpect(jsonPath("$.data.sections[1].section").value("SW"))
        .andExpect(jsonPath("$.data.sections[1].staffs").isEmpty())
        .andExpect(jsonPath("$.data.sections[2].section").value("STARTUP"));
  }

  @Test
  @DisplayName("활성 기수가 없어도 200 이고 3개 section 모두 빈 배열이다")
  void returnsEmptySectionsWhenNoActiveGeneration() throws Exception {
    mockMvc.perform(get(STAFFS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.sections.length()").value(3))
        .andExpect(jsonPath("$.data.sections[0].staffs").isEmpty());
  }
}
