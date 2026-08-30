package com.getit.domain.recruitment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.dto.BulkDecisionRequest;
import com.getit.domain.recruitment.dto.DocumentDecisionRequest;
import com.getit.domain.recruitment.dto.EvaluationScoreItem;
import com.getit.domain.recruitment.dto.EvaluationScoreSaveRequest;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** 7.1~7.6 /api/admin/recruitment/applications */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationAdminControllerTest {

  private static final String APPLICATIONS_PATH = "/api/admin/recruitment/applications";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  @Autowired
  private EvaluationCriterionRepository evaluationCriterionRepository;

  @Autowired
  private ObjectMapper objectMapper;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private String adminToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "admin@getit.com", Role.ADMIN);
  }

  private Application draft(Long userId, String name) {
    return applicationRepository.save(Application.createDraft(
        userId, activeGeneration.getId(), name, name + "@gmail.com", "010-1234-5678",
        null, null, 2, "2021110000"));
  }

  private Application submitted(Long userId, String name) {
    Application application = draft(userId, name);
    application.submit(LocalDateTime.now());
    return application;
  }

  private Application submittedAt(Long userId, String name, LocalDateTime submittedAt) {
    Application application = draft(userId, name);
    application.submit(submittedAt);
    return application;
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(APPLICATIONS_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ADMIN 이 아니면 403 이다")
    void rejectsNonAdmin() throws Exception {
      String token = "Bearer " + jwtProvider.createAccessToken(1L, "member@getit.com", Role.MEMBER);

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", token))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH)
  class GetApplicants {

    @Test
    @DisplayName("DRAFT 를 제외한 지원자 목록을 페이징 응답으로 반환한다")
    void returnsApplicantsExcludingDraft() throws Exception {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("홍길동"))
          .andExpect(jsonPath("$.data.content[1]").doesNotExist())
          .andExpect(jsonPath("$.data.totalElements").value(1))
          .andExpect(jsonPath("$.data.page").value(0));
    }

    @Test
    @DisplayName("status 로 필터링한다")
    void filtersByStatus() throws Exception {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      mockMvc.perform(get(APPLICATIONS_PATH)
              .header("Authorization", adminToken())
              .param("status", "DRAFT"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("김철수"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("generationId 로 필터링하면 비활성 기수도 조회할 수 있다")
    void filtersByGenerationId() throws Exception {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      Application other = applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));
      other.submit(LocalDateTime.now());

      mockMvc.perform(get(APPLICATIONS_PATH)
              .header("Authorization", adminToken())
              .param("generationId", String.valueOf(otherGeneration.getId())))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].name").value("지난기수"))
          .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("submittedAt 이 같아도 id 로 안정적으로 정렬된다")
    void stableSortWhenSubmittedAtTies() throws Exception {
      LocalDateTime sameInstant = LocalDateTime.now();
      Application first = submitted(1L, "가나다");
      Application second = submitted(2L, "마바사");
      ReflectionTestUtils.setField(first, "submittedAt", sameInstant);
      ReflectionTestUtils.setField(second, "submittedAt", sameInstant);

      mockMvc.perform(get(APPLICATIONS_PATH).header("Authorization", adminToken()).param("size", "1"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(second.getId()));
    }

    @Test
    @DisplayName("?sort= 로 다른 정렬을 요청해도 무시하고 강제된 순서로 응답한다")
    void ignoresClientSuppliedSort() throws Exception {
      LocalDateTime tie = LocalDateTime.of(2026, 9, 1, 11, 0);
      submittedAt(1L, "가나다", tie);
      Application higherId = submittedAt(2L, "다바가", tie);

      mockMvc.perform(get(APPLICATIONS_PATH)
              .header("Authorization", adminToken())
              .param("sort", "name,asc"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.content[0].id").value(higherId.getId()));
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH + "/{id}")
  class GetApplicantDetail {

    @Test
    @DisplayName("지원자 상세를 반환한다")
    void returnsDetail() throws Exception {
      Application application = submitted(1L, "홍길동");
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + application.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(application.getId()))
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
          .andExpect(jsonPath("$.data.basicInfo.name").value("홍길동"))
          .andExpect(jsonPath("$.data.answers[0].answerText").value("지원 동기입니다."));
    }

    @Test
    @DisplayName("없는 지원서면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(get(APPLICATIONS_PATH + "/999").header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("DRAFT 상태면 404 다")
    void returns404WhenDraft() throws Exception {
      Application application = draft(1L, "홍길동");

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + application.getId())
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("PUT " + APPLICATIONS_PATH + "/{id}/scores")
  class SaveScores {

    private String scoresRequestJson(Long criterionId, int score) throws Exception {
      return objectMapper.writeValueAsString(
          new EvaluationScoreSaveRequest(List.of(new EvaluationScoreItem(criterionId, score))));
    }

    @Test
    @DisplayName("점수를 저장한다")
    void savesScores() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 45)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.totalScore").value(45))
          // 응답이 종합 결과로 바뀌었다. 내 점수는 myScore 로 온다 (이슈 #151).
          .andExpect(jsonPath("$.data.criteria[0].myScore").value(45))
          .andExpect(jsonPath("$.data.criteria[0].evaluatorScores[0].score").value(45))
          .andExpect(jsonPath("$.data.evaluatorCount").value(1));
    }

    @Test
    @DisplayName("배점을 초과하면 400 이다")
    void returns400WhenScoreExceedsMax() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 61)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("SCORE_EXCEEDS_MAX"));
    }

    @Test
    @DisplayName("존재하지 않는 기준이면 404 다")
    void returns404WhenCriterionNotFound() throws Exception {
      Application application = submitted(1L, "홍길동");

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(999L, 10)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("CRITERION_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 합불이 결정된 지원서는 409 다")
    void returns409WhenAlreadyDecided() throws Exception {
      Application application = submitted(1L, "홍길동");
      EvaluationCriterion criterion = evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "전공 적합성", "가이드 라인", 60));
      applicationRepository.updateStatusIfCurrentStatus(
          application.getId(), ApplicationStatus.DOC_PASS, ApplicationStatus.SUBMITTED);

      mockMvc.perform(put(APPLICATIONS_PATH + "/" + application.getId() + "/scores")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(scoresRequestJson(criterion.getId(), 10)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_SCORABLE"));
    }
  }

  @Nested
  @DisplayName("PATCH " + APPLICATIONS_PATH + "/{id}/decision")
  class Decide {

    private String decisionRequestJson(boolean passed) throws Exception {
      return objectMapper.writeValueAsString(new DocumentDecisionRequest(passed));
    }

    @Test
    @DisplayName("합불을 결정한다")
    void decides() throws Exception {
      Application application = submitted(1L, "홍길동");

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("DOC_PASS"));
    }

    @Test
    @DisplayName("DRAFT 상태면 404 다 (존재하지 않는 것과 동일하게 취급)")
    void returns404WhenDraft() throws Exception {
      Application application = draft(1L, "홍길동");

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("DOC_PASS 상태에서 다시 결정하면 최종 합불로 전이한다 (7.4 확장)")
    void decidesFinalResultFromDocPass() throws Exception {
      Application application = submitted(1L, "홍길동");
      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("DOC_PASS"));

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("FINAL_PASS"));
    }

    @Test
    @DisplayName("이미 최종 결정된 지원서를 다시 결정하려 하면 409 다")
    void returns409WhenAlreadyFinalDecided() throws Exception {
      Application application = submitted(1L, "홍길동");
      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk());
      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(true)))
          .andExpect(status().isOk());

      mockMvc.perform(patch(APPLICATIONS_PATH + "/" + application.getId() + "/decision")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(decisionRequestJson(false)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_SUBMITTED"));
    }
  }

  @Nested
  @DisplayName("PUT " + APPLICATIONS_PATH + "/status")
  class DecideBulk {

    private String bulkRequestJson(List<Long> applicationIds, ApplicationStatus status) throws Exception {
      return objectMapper.writeValueAsString(new BulkDecisionRequest(applicationIds, status));
    }

    @Test
    @DisplayName("SUBMITTED 인 지원서들을 일괄로 DOC_PASS 처리한다")
    void decidesBulk() throws Exception {
      Application first = submitted(1L, "홍길동");
      Application second = submitted(2L, "김철수");

      mockMvc.perform(put(APPLICATIONS_PATH + "/status")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(bulkRequestJson(List.of(first.getId(), second.getId()), ApplicationStatus.DOC_PASS)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.updatedCount").value(2))
          .andExpect(jsonPath("$.data.status").value("DOC_PASS"));
    }

    @Test
    @DisplayName("목표 status 가 유효하지 않으면 400 이다")
    void returns400WhenTargetStatusInvalid() throws Exception {
      Application application = submitted(1L, "홍길동");

      mockMvc.perform(put(APPLICATIONS_PATH + "/status")
              .header("Authorization", adminToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(bulkRequestJson(List.of(application.getId()), ApplicationStatus.SUBMITTED)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("INVALID_DECISION_STATUS"));
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH + "/{id}/adjacent")
  class GetAdjacentApplicants {

    @Test
    @DisplayName("이전 · 다음 지원서 id 를 반환한다")
    void returnsPreviousAndNext() throws Exception {
      Application oldest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 0));
      Application middle = submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 11, 0));
      Application newest = submittedAt(3L, "이영희", LocalDateTime.of(2026, 9, 1, 12, 0));

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + middle.getId() + "/adjacent")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.previousApplicationId").value(newest.getId()))
          .andExpect(jsonPath("$.data.nextApplicationId").value(oldest.getId()));
    }

    @Test
    @DisplayName("submittedAt 이 같으면 id 내림차순으로 tie-break 한다")
    void tieBreaksById() throws Exception {
      LocalDateTime tie = LocalDateTime.of(2026, 9, 1, 11, 0);
      Application oldest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 0));
      Application lowerId = submittedAt(2L, "김철수", tie);
      Application higherId = submittedAt(3L, "이영희", tie);

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + lowerId.getId() + "/adjacent")
              .header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.previousApplicationId").value(higherId.getId()))
          .andExpect(jsonPath("$.data.nextApplicationId").value(oldest.getId()));
    }

    @Test
    @DisplayName("DRAFT 상태면 404 다")
    void returns404WhenDraft() throws Exception {
      Application application = draft(1L, "홍길동");

      mockMvc.perform(get(APPLICATIONS_PATH + "/" + application.getId() + "/adjacent")
              .header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("없는 지원서면 404 다")
    void returns404WhenNotFound() throws Exception {
      mockMvc.perform(get(APPLICATIONS_PATH + "/999/adjacent").header("Authorization", adminToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET " + APPLICATIONS_PATH + "/excel")
  class DownloadExcel {

    @Test
    @DisplayName("지원자 목록을 엑셀 파일로 반환한다")
    void downloadsExcel() throws Exception {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      byte[] excel = mockMvc.perform(get(APPLICATIONS_PATH + "/excel").header("Authorization", adminToken()))
          .andExpect(status().isOk())
          .andExpect(header().string(
              "Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
          .andReturn().getResponse().getContentAsByteArray();

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getLastRowNum()).isEqualTo(1);
        assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("홍길동");
      }
    }
  }
}
