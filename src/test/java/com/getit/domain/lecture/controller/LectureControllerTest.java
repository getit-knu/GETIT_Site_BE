package com.getit.domain.lecture.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.dto.LectureRequest.Create;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
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

/** 8.1~8.3 /api/admin/lectures */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LectureControllerTest {

  private static final String LECTURES_PATH = "/api/admin/lectures";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private LectureRepository lectureRepository;

  private Long activeGenerationId;
  private Long trackId;
  private Long subCategoryId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();

    trackId = trackRepository.save(Track.create("SW", 1)).getId();
    subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(2L, "member@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(LECTURES_PATH))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      mockMvc.perform(get(LECTURES_PATH).header("Authorization", memberToken()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST " + LECTURES_PATH)
  class CreateLecture {

    @Test
    @DisplayName("생성에 성공하면 201과 강의 정보를 반환한다")
    void createsLecture() throws Exception {
      Create request = new Create(
          null, trackId, subCategoryId, 1, "HTML/CSS 기초", "## 학습 구성",
          "https://youtube.com/watch?v=abc123", "https://docs.getit.com/web-basic", 120,
          null, true, null);

      mockMvc.perform(post(LECTURES_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.title").value("HTML/CSS 기초"))
          .andExpect(jsonPath("$.data.week").value(1));
    }

    @Test
    @DisplayName("title이 비어 있으면 400 이다")
    void returns400WhenTitleBlank() throws Exception {
      Create request = new Create(
          null, trackId, subCategoryId, 1, "", null, null, null, null, null, true, null);

      mockMvc.perform(post(LECTURES_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("존재하지 않는 트랙이면 404 이다")
    void returns404WhenTrackNotFound() throws Exception {
      Create request = new Create(
          null, 999_999L, null, 1, "HTML/CSS 기초", null, null, null, null, null, true, null);

      mockMvc.perform(post(LECTURES_PATH)
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("TRACK_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET " + LECTURES_PATH)
  class GetLectures {

    @Test
    @DisplayName("트랙·소분류 트리와 강의 목록을 반환한다")
    void returnsLecturesWithTrackTree() throws Exception {
      lectureRepository.save(Lecture.create(
          1, "HTML/CSS 기초", null, null, null, null, true, activeGenerationId, trackId, subCategoryId, 1L));

      mockMvc.perform(get(LECTURES_PATH)
              .header("Authorization", adminToken())
              .param("trackId", trackId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.tracks[0].name").value("SW"))
          .andExpect(jsonPath("$.data.lectures[0].title").value("HTML/CSS 기초"));
    }
  }

  @Nested
  @DisplayName("GET " + LECTURES_PATH + "/{id}")
  class GetLecture {

    @Test
    @DisplayName("단건 조회에 성공한다")
    void returnsLectureDetail() throws Exception {
      Lecture lecture = lectureRepository.save(Lecture.create(
          1, "HTML/CSS 기초", null, null, null, null, true, 9L, trackId, subCategoryId, 1L));

      mockMvc.perform(get(LECTURES_PATH + "/" + lecture.getId()).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.title").value("HTML/CSS 기초"))
          .andExpect(jsonPath("$.data.trackId").value(trackId));
    }

    @Test
    @DisplayName("없는 강의 id면 404 이다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(get(LECTURES_PATH + "/999999").header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("LECTURE_NOT_FOUND"));
    }
  }
}
