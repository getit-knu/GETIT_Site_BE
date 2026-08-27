package com.getit.domain.lecture.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.lecture.dto.SubmissionRequest;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
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
class SubmissionControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  private Long assignmentId;

  @BeforeEach
  void setUp() {
    assignmentId = assignmentRepository.save(Assignment.create(
        1L, "과제", "설명", LocalDateTime.now().plusDays(7), Set.of(SubmissionType.LINK), null)).getId();
  }

  private String memberToken() {
    return "Bearer " + jwtProvider.createAccessToken(100L, "member@getit.com", Role.MEMBER);
  }

  private String otherMemberToken() {
    return "Bearer " + jwtProvider.createAccessToken(200L, "other@getit.com", Role.MEMBER);
  }

  @Nested
  @DisplayName("POST /api/member/assignments/{assignmentId}/submissions")
  class Submit {

    @Test
    @DisplayName("제출에 성공하면 201과 제출 정보를 반환한다")
    void submitsAssignment() throws Exception {
      SubmissionRequest.Submit request = new SubmissionRequest.Submit(null, "https://github.com/user/repo", "완료");

      mockMvc.perform(post("/api/member/assignments/" + assignmentId + "/submissions")
              .header("Authorization", memberToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.data.linkUrl").value("https://github.com/user/repo"))
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      SubmissionRequest.Submit request = new SubmissionRequest.Submit(null, "https://github.com/user/repo", null);

      mockMvc.perform(post("/api/member/assignments/" + assignmentId + "/submissions")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("존재하지 않는 과제면 404 이다")
    void returns404WhenAssignmentNotFound() throws Exception {
      SubmissionRequest.Submit request = new SubmissionRequest.Submit(null, "https://github.com/user/repo", null);

      mockMvc.perform(post("/api/member/assignments/999999/submissions")
              .header("Authorization", memberToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("ASSIGNMENT_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT /api/member/submissions/{id}")
  class Resubmit {

    private Long submitAndGetId() {
      AssignmentSubmission submission = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, 100L, null, "https://github.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));
      return submission.getId();
    }

    @Test
    @DisplayName("재제출에 성공하면 200과 갱신된 정보를 반환한다")
    void resubmitsAssignment() throws Exception {
      Long submissionId = submitAndGetId();
      SubmissionRequest.Submit request = new SubmissionRequest.Submit(
          null, "https://gitlab.com/user/repo", "수정함");

      mockMvc.perform(put("/api/member/submissions/" + submissionId)
              .header("Authorization", memberToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.linkUrl").value("https://gitlab.com/user/repo"));
    }

    @Test
    @DisplayName("본인 제출물이 아니면 403 이다")
    void returns403WhenNotOwner() throws Exception {
      Long submissionId = submitAndGetId();
      SubmissionRequest.Submit request = new SubmissionRequest.Submit(null, "https://gitlab.com/user/repo", null);

      mockMvc.perform(put("/api/member/submissions/" + submissionId)
              .header("Authorization", otherMemberToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isForbidden())
          .andExpect(jsonPath("$.error.code").value("NOT_RESOURCE_OWNER"));
    }
  }
}
