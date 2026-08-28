package com.getit.domain.lecture.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
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
import java.time.LocalDateTime;
import java.util.Set;
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
class SubmissionAdminControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private UserRepository userRepository;

  private Long lectureId;
  private Long assignmentId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
    lectureId = lectureRepository.save(Lecture.create(
        1, "테스트 강의", null, null, null, null, true, generation.getId(), trackId, subCategoryId, 1L)).getId();
    assignmentId = assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", LocalDateTime.now().plusDays(7), Set.of(SubmissionType.LINK), null)).getId();
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(2L, "member@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("GET /api/admin/lectures/{id}/submissions")
  class GetOverview {

    @Test
    @DisplayName("조회에 성공하면 200과 통계를 반환한다")
    void returnsOverview() throws Exception {
      User member = User.createGuest("sub-c1", "sub-c1@getit.com", "제출자", null);
      member.promoteToMember(9);
      userRepository.save(member);
      assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, member.getId(), null, "https://github.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));

      mockMvc.perform(get("/api/admin/lectures/" + lectureId + "/submissions")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.counts.total").value(1))
          .andExpect(jsonPath("$.data.counts.submitted").value(1));
    }

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get("/api/admin/lectures/" + lectureId + "/submissions"))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      mockMvc.perform(get("/api/admin/lectures/" + lectureId + "/submissions")
              .header("Authorization", memberToken()))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /api/admin/submissions/{id}")
  class GetDetail {

    @Test
    @DisplayName("존재하지 않는 제출물이면 404 이다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(get("/api/admin/submissions/999999").header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SUBMISSION_NOT_FOUND"));
    }
  }
}
