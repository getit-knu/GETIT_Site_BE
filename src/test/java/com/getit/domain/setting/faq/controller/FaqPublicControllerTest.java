package com.getit.domain.setting.faq.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.repository.FaqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 2.5 GET /api/public/faqs */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FaqPublicControllerTest {

  private static final String FAQS_PATH = "/api/public/faqs";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private FaqRepository faqRepository;

  @Test
  @DisplayName("인증 없이 노출 FAQ 만 order 순으로 반환한다")
  void returnsVisibleFaqsWithoutAuthentication() throws Exception {
    faqRepository.save(Faq.create(new FaqCommand("B", "답변B", true), 2));
    faqRepository.save(Faq.create(new FaqCommand("A", "답변A", true), 1));
    faqRepository.save(Faq.create(new FaqCommand("숨김", "답변", false), 3));

    mockMvc.perform(get(FAQS_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(2))
        .andExpect(jsonPath("$.data[0].question").value("A"))
        .andExpect(jsonPath("$.data[0].order").value(1))
        .andExpect(jsonPath("$.data[1].question").value("B"));
  }
}
