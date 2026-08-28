package com.getit.domain.qna.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.qna.admin.dto.AdminAnswerRequest;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestionAdminControllerTest {

  private static final String PATH = "/api/admin/questions";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private UserRepository userRepository;

  private Long adminId;

  @BeforeEach
  void setUp() {
    User admin = User.createGuest("admin", "admin@getit.com", "관리자", null);
    admin.updateRole(Role.ADMIN);
    adminId = userRepository.save(admin).getId();
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(adminId, "admin@getit.com", Role.ADMIN);
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void rejectsAnonymous() throws Exception {
    mockMvc.perform(get(PATH)).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("ADMIN 이 아니면 403 이다")
  void rejectsNonAdmin() throws Exception {
    String token = "Bearer " + jwtProvider.createAccessToken(adminId, "admin@getit.com", Role.MEMBER);

    mockMvc.perform(get(PATH).header("Authorization", token)).andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("lectureId 가 none·숫자가 아니면 400 이다")
  void rejectsMalformedLectureId() throws Exception {
    mockMvc.perform(get(PATH).param("lectureId", "abc").header("Authorization", adminToken()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
  }

  @Test
  @DisplayName("답변을 달면 201 이고 questionStatus 가 ANSWERED 다")
  void createsAnswer() throws Exception {
    Long questionId = questionRepository.save(Question.create(1L, null, "질문")).getId();

    mockMvc.perform(post(PATH + "/" + questionId + "/answer")
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new AdminAnswerRequest.Write("답변드립니다"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.questionStatus").value("ANSWERED"));
  }

  @Test
  @DisplayName("이미 답변된 질문에 또 달면 409 다")
  void rejectsDuplicateAnswer() throws Exception {
    Long questionId = questionRepository.save(Question.create(1L, null, "질문")).getId();
    String answerBody = objectMapper.writeValueAsString(new AdminAnswerRequest.Write("답변"));

    mockMvc.perform(post(PATH + "/" + questionId + "/answer")
        .header("Authorization", adminToken()).contentType(MediaType.APPLICATION_JSON).content(answerBody));

    mockMvc.perform(post(PATH + "/" + questionId + "/answer")
            .header("Authorization", adminToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(answerBody))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("ALREADY_ANSWERED"));
  }
}
