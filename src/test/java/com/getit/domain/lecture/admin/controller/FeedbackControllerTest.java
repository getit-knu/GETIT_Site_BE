package com.getit.domain.lecture.admin.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.admin.dto.FeedbackRequest;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDateTime;
import java.util.Set;
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

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedbackControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private FeedbackRepository feedbackRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  private Long submissionId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
    Long lectureId = lectureRepository.save(Lecture.create(
        1, "테스트 강의", null, null, null, null, true, generation.getId(), trackId, subCategoryId, 1L)).getId();
    Long assignmentId = assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", LocalDateTime.now().plusDays(7), Set.of(SubmissionType.LINK), null)).getId();
    submissionId = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignmentId, 100L, null, "https://github.com/user/repo", null,
        SubmissionStatus.SUBMITTED, LocalDateTime.now())).getId();
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(2L, "member@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("POST /api/admin/submissions/{id}/feedback")
  class Create {

    @Test
    @DisplayName("작성에 성공하면 201을 반환한다")
    void createsFeedback() throws Exception {
      FeedbackRequest.Write request = new FeedbackRequest.Write("잘했습니다");

      mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/feedback")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.content").value("잘했습니다"));
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      FeedbackRequest.Write request = new FeedbackRequest.Write("잘했습니다");

      mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/feedback")
              .header("Authorization", memberToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      FeedbackRequest.Write request = new FeedbackRequest.Write("잘했습니다");

      mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/feedback")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("내용이 비어 있으면 400 이다")
    void returns400WhenContentBlank() throws Exception {
      FeedbackRequest.Write request = new FeedbackRequest.Write("");

      mockMvc.perform(post("/api/admin/submissions/" + submissionId + "/feedback")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
  }

  @Nested
  @DisplayName("PUT /api/admin/feedbacks/{feedbackId}")
  class Update {

    @Test
    @DisplayName("작성자 본인이면 수정된다")
    void updatesFeedback() throws Exception {
      Feedback feedback = feedbackRepository.save(Feedback.create(submissionId, 1L, "원래 내용"));
      FeedbackRequest.Write request = new FeedbackRequest.Write("수정된 내용");

      mockMvc.perform(put("/api/admin/feedbacks/" + feedback.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    @DisplayName("작성자가 아니면 403 이다")
    void returns403WhenNotAuthor() throws Exception {
      Feedback feedback = feedbackRepository.save(Feedback.create(submissionId, 999L, "원래 내용"));
      FeedbackRequest.Write request = new FeedbackRequest.Write("수정된 내용");

      mockMvc.perform(put("/api/admin/feedbacks/" + feedback.getId())
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("NOT_RESOURCE_OWNER"));
    }
  }
}
