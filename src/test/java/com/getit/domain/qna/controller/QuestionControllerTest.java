package com.getit.domain.qna.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.qna.dto.MemberQuestionRequest;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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
class QuestionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private QuestionRepository questionRepository;

  private Long lectureId;
  private Long memberId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    Long generationId = generationRepository.save(generation).getId();
    lectureId = lectureRepository.save(Lecture.create(
        1, "1주차", null, null, null, null, true, generationId, null, null, 1L)).getId();
    User member = User.createGuest("m", "m@getit.com", "부원", null);
    member.promoteToMember(9);
    memberId = userRepository.save(member).getId();
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(memberId, "m@getit.com", Role.MEMBER);
  }

  private String body(String content) throws Exception {
    return objectMapper.writeValueAsString(new MemberQuestionRequest.Create(content));
  }

  @Test
  @DisplayName("토큰이 없으면 401 이다")
  void rejectsAnonymous() throws Exception {
    mockMvc.perform(get("/api/member/lectures/" + lectureId + "/questions"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GUEST 면 403 이다")
  void rejectsGuest() throws Exception {
    String token = "Bearer " + jwtProvider.createAccessToken(memberId, "m@getit.com", Role.GUEST);

    mockMvc.perform(get("/api/member/lectures/" + lectureId + "/questions").header("Authorization", token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("질문을 등록하고 목록에서 조회한다")
  void createsAndListsOwnQuestion() throws Exception {
    mockMvc.perform(post("/api/member/lectures/" + lectureId + "/questions")
            .header("Authorization", memberToken())
            .contentType(MediaType.APPLICATION_JSON)
            .content(body("질문이요")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.status").value("PENDING"));

    mockMvc.perform(get("/api/member/lectures/" + lectureId + "/questions")
            .header("Authorization", memberToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].content").value("질문이요"));
  }

  @Test
  @DisplayName("GET /api/member/questions — 내 질문을 강의 정보와 함께 준다")
  void listsMyQuestionsAcrossLectures() throws Exception {
    questionRepository.save(Question.create(memberId, lectureId, "내 질문"));

    mockMvc.perform(get("/api/member/questions").header("Authorization", memberToken()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content[0].content").value("내 질문"))
        .andExpect(jsonPath("$.data.content[0].lectureId").value(lectureId))
        .andExpect(jsonPath("$.data.content[0].lectureTitle").value("1주차"));
  }

  @Test
  @DisplayName("GET /api/member/questions — 토큰이 없으면 401 이다")
  void myQuestionsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/member/questions")).andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("GET /api/member/questions — GUEST 는 쓸 수 없다")
  void myQuestionsRejectGuest() throws Exception {
    String guestToken = "Bearer " + jwtProvider.createAccessToken(memberId, "m@getit.com", Role.GUEST);

    mockMvc.perform(get("/api/member/questions").header("Authorization", guestToken))
        .andExpect(status().isForbidden());
  }
}
