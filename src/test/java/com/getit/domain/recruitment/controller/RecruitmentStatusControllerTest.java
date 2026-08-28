package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.8 GET /api/public/recruitment/status */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecruitmentStatusControllerTest {

  private static final String STATUS_PATH = "/api/public/recruitment/status";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Test
  @DisplayName("인증 없이 조회할 수 있고, 서류 접수 기간이면 DOCUMENT_OPEN 을 반환한다")
  void returnsDocumentOpenWithoutAuthentication() throws Exception {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generation.getId(), now.minusDays(5), now.plusDays(20),
        now.minusDays(5), now.plusDays(5), now.plusDays(10)));

    mockMvc.perform(get(STATUS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.phase").value("DOCUMENT_OPEN"))
        .andExpect(jsonPath("$.data.applyEnabled").value(true))
        .andExpect(jsonPath("$.data.generationNo").value(9));
  }

  @Test
  @DisplayName("활성 기수가 없어도 200 이고 CLOSED 다")
  void returnsClosedWhenNoActiveGeneration() throws Exception {
    mockMvc.perform(get(STATUS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.phase").value("CLOSED"))
        .andExpect(jsonPath("$.data.applyEnabled").value(false));
  }
}
