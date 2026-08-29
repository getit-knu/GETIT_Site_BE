package com.getit.domain.setting.home.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.1 GET /api/public/home */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class HomeControllerTest {

  private static final String HOME_PATH = "/api/public/home";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private CurriculumRepository curriculumRepository;

  @Test
  @DisplayName("인증 없이 조회할 수 있고, 활성 기수 정보와 커리큘럼을 반환한다")
  void returnsHomeWithoutAuthentication() throws Exception {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    curriculumRepository.save(Curriculum.create(generation.getId(), 1, "Python & 데이터 분석", "부제"));

    mockMvc.perform(get(HOME_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.generation.generationNo").value(9))
        .andExpect(jsonPath("$.data.curriculums[0].title").value("Python & 데이터 분석"))
        .andExpect(jsonPath("$.data.faqs").isArray())
        .andExpect(jsonPath("$.data.featuredProjects").isArray())
        .andExpect(jsonPath("$.data.features.stockGame").value(false))
        .andExpect(jsonPath("$.data.features.mockInvestment").value(false));
  }

  @Test
  @DisplayName("활성 기수가 없어도 200 이고, generation 은 null, recruitment 는 CLOSED 다")
  void returnsGracefulResultWhenNoActiveGeneration() throws Exception {
    mockMvc.perform(get(HOME_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.generation").value(nullValue()))
        .andExpect(jsonPath("$.data.recruitment.phase").value("CLOSED"))
        .andExpect(jsonPath("$.data.curriculums").isEmpty());
  }
}
