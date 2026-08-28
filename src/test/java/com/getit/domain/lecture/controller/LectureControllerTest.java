package com.getit.domain.lecture.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LectureControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private UserRepository userRepository;

  private Long lectureId;
  private Long memberId;
  private Long outsiderId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    Long activeGenerationId = generationRepository.save(generation).getId();
    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("WEB 기초", 1, trackId)).getId();

    memberId = member("member", 9).getId();
    outsiderId = member("outsider", 8).getId();
    lectureId = lectureRepository.save(Lecture.create(
        1, "1주차", "본문", null, null, 60, true, activeGenerationId, trackId, subCategoryId, memberId)).getId();
  }

  private User member(String providerId, int generationNo) {
    User user = User.createGuest(providerId, providerId + "@getit.com", providerId, null);
    user.promoteToMember(generationNo);
    return userRepository.save(user);
  }

  private String token(Long userId) {
    return "Bearer " + jwtProvider.createAccessToken(userId, userId + "@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("GET /api/member/lectures")
  class GetLectures {

    @Test
    @DisplayName("토큰 없으면 401")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get("/api/member/lectures"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("활성 기수 부원이면 200")
    void returnsLectures() throws Exception {
      mockMvc.perform(get("/api/member/lectures").header("Authorization", token(memberId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(lectureId));
    }

    @Test
    @DisplayName("다른 기수 부원이면 403")
    void forbidsOutsider() throws Exception {
      mockMvc.perform(get("/api/member/lectures").header("Authorization", token(outsiderId)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/member/lectures/{id}")
  class GetLecture {

    @Test
    @DisplayName("활성 기수 부원이면 200")
    void returnsLecture() throws Exception {
      mockMvc.perform(get("/api/member/lectures/" + lectureId).header("Authorization", token(memberId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(lectureId))
          .andExpect(jsonPath("$.data.mySubmission").doesNotExist());
    }

    @Test
    @DisplayName("없는 강의면 404")
    void notFound() throws Exception {
      mockMvc.perform(get("/api/member/lectures/999999").header("Authorization", token(memberId)))
          .andExpect(status().isNotFound());
    }
  }

  @Nested
  @DisplayName("GET /api/member/lectures/{lectureId}/materials/{fileId}/download")
  class DownloadMaterial {

    @Test
    @DisplayName("연결 안 된 파일이면 404")
    void notFoundWhenFileNotLinked() throws Exception {
      mockMvc.perform(get("/api/member/lectures/" + lectureId + "/materials/123/download")
              .header("Authorization", token(memberId)))
          .andExpect(status().isNotFound());
    }
  }
}
