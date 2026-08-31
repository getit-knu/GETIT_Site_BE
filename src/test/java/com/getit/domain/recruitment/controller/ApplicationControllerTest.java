package com.getit.domain.recruitment.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.domain.recruitment.dto.ApplicationAnswerRequest;
import com.getit.domain.recruitment.dto.ApplicationDraftRequest;
import com.getit.domain.recruitment.dto.BasicInfo;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationQuestionRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import java.time.LocalDateTime;
import java.util.List;
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

/** 3.1~3.5 /api/applications */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApplicationControllerTest {

  private static final String FORM_PATH = "/api/applications/form";
  private static final String ME_PATH = "/api/applications/me";
  private static final String DRAFT_PATH = "/api/applications/me/draft";
  private static final String SUBMIT_PATH = "/api/applications/me/submit";
  private static final String RESULT_PATH = "/api/applications/me/result";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JwtProvider jwtProvider;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private void saveSchedule() {
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 30, 23, 59, 59),
        LocalDateTime.of(2026, 9, 1, 0, 0),
        LocalDateTime.of(2026, 9, 10, 23, 59, 59),
        LocalDateTime.of(2026, 9, 15, 0, 0)));
  }

  /** now 가 서류 접수 기간 안에 들어오는 일정. 실제 시각과 무관하게 항상 열려 있다. */
  private void saveOpenSchedule() {
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        now.minusDays(10), now.plusDays(20),
        now.minusDays(5), now.plusDays(5),
        now.plusDays(10)));
  }

  /** now 가 서류 접수 시작 전인 일정. (이슈 #175) */
  private void saveNotOpenYetSchedule() {
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        now.plusDays(1), now.plusDays(30),
        now.plusDays(3), now.plusDays(10),
        now.plusDays(20)));
  }

  /** now 가 서류 접수 마감을 지난 일정. */
  private void saveClosedSchedule() {
    LocalDateTime now = LocalDateTime.now();
    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        activeGeneration.getId(),
        now.minusDays(30), now.minusDays(1),
        now.minusDays(30), now.minusDays(20),
        now.minusDays(10)));
  }

  private String guestToken() {
    return "Bearer " + jwtProvider.createAccessToken(1L, "guest@getit.com", Role.GUEST);
  }

  private String draftRequestJson(List<ApplicationAnswerRequest> answers) throws Exception {
    BasicInfo basicInfo = new BasicInfo("홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, "2021110000");
    return objectMapper.writeValueAsString(new ApplicationDraftRequest(basicInfo, answers));
  }

  @Nested
  @DisplayName("인가")
  class Authorization {

    @Test
    @DisplayName("토큰이 없으면 401 이다")
    void rejectsAnonymous() throws Exception {
      mockMvc.perform(get(FORM_PATH)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GUEST 도 접근할 수 있다")
    void allowsGuest() throws Exception {
      saveSchedule();

      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("GET " + FORM_PATH)
  class GetForm {

    @Test
    @DisplayName("기수 · 단계 · 마감일 · 질문 목록을 반환한다")
    void returnsForm() throws Exception {
      saveSchedule();
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, 300, null));

      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.generationNo").value(9))
          .andExpect(jsonPath("$.data.deadline").value("2026-09-10T23:59:59+09:00"))
          .andExpect(jsonPath("$.data.questions[0].content").value("지원 동기"))
          .andExpect(jsonPath("$.data.questions[0].placeholder").doesNotExist());
    }

    @Test
    @DisplayName("모집 일정이 없으면 404 다")
    void returns404WhenNoSchedule() throws Exception {
      mockMvc.perform(get(FORM_PATH).header("Authorization", guestToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"));
    }
  }

  @Nested
  @DisplayName("GET " + ME_PATH)
  class GetMyApplication {

    @Test
    @DisplayName("지원서가 없으면 data 가 null 이다")
    void returnsNullWhenNoApplication() throws Exception {
      mockMvc.perform(get(ME_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("내 지원서와 답변을 반환한다")
    void returnsMyApplication() throws Exception {
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678",
          null, null, 2, "2021110000"));
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      mockMvc.perform(get(ME_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.id").value(application.getId()))
          .andExpect(jsonPath("$.data.status").value("DRAFT"))
          .andExpect(jsonPath("$.data.basicInfo.name").value("홍길동"))
          .andExpect(jsonPath("$.data.answers[0].answerText").value("지원 동기입니다."));
    }
  }

  @Nested
  @DisplayName("PUT " + DRAFT_PATH)
  class SaveDraft {

    @Test
    @DisplayName("임시 저장한다")
    void savesDraft() throws Exception {
      saveOpenSchedule();

      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(List.of(new ApplicationAnswerRequest(10L, "지원 동기입니다.", null)))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("DRAFT"))
          .andExpect(jsonPath("$.data.savedAt").exists());
    }

    @Test
    @DisplayName("이미 제출된 지원서면 409 다")
    void returns409WhenAlreadySubmitted() throws Exception {
      saveOpenSchedule();
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, null));
      application.submit(LocalDateTime.now());

      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.error.code").value("ALREADY_SUBMITTED"));
    }

    @Test
    @DisplayName("서류 접수 기간이 아니면 422 다")
    void returns422WhenDeadlinePassed() throws Exception {
      saveClosedSchedule();

      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_DEADLINE_PASSED"));
    }

    @Test
    @DisplayName("모집 시작 전이면 기한 지남이 아니라 모집 기간 아님으로 막는다")
    void returns422NotOpenBeforeStart() throws Exception {
      saveNotOpenYetSchedule();

      // 하나로 묶으면 시작 직전에 들어온 지원자가 "제출 기한이 지났습니다" 를 본다 (이슈 #175).
      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_OPEN"));
    }

    @Test
    @DisplayName("단과대 · 학과 id 가 실제로 저장되고 다시 읽힌다")
    void persistsCollegeAndMajorIds() throws Exception {
      saveOpenSchedule();
      // 배관만 보는 테스트다. 마스터 데이터에 있을 법한 값을 쓰면 시드가 바뀔 때
      // 뜻 없이 깨진다 (PR #191 리뷰 지적).
      BasicInfo basicInfo = new BasicInfo(
          "홍길동", "hong@gmail.com", "010-1234-5678", 777L, 888L, 2, "2021110000");
      String body = objectMapper.writeValueAsString(
          new ApplicationDraftRequest(basicInfo, null));

      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(body))
          .andExpect(status().isOk());

      // 주석에는 "마스터 데이터가 없어 항상 null" 이라고 적혀 있었지만, 배관은 동작한다.
      // 비는 이유는 지원서 폼이 id 를 담아 보내지 않기 때문이다 (이슈 #184).
      mockMvc.perform(get(ME_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.basicInfo.collegeId").value(777))
          .andExpect(jsonPath("$.data.basicInfo.majorId").value(888));
    }

    @Test
    @DisplayName("지원 스위치가 내려가 있으면 422 다")
    void returns422WhenApplyPaused() throws Exception {
      saveOpenSchedule();
      RecruitmentSchedule schedule =
          recruitmentScheduleRepository.findByGenerationId(activeGeneration.getId()).orElseThrow();
      schedule.changeApplyEnabled(false);
      recruitmentScheduleRepository.saveAndFlush(schedule);

      // 공개 화면 표시만 바꾸고 여기가 열려 있으면 스위치가 의미가 없다 (이슈 #170).
      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_PAUSED"));
    }

    @Test
    @DisplayName("컬럼이 감당하지 못할 만큼 긴 답변은 400 이다")
    void rejectsAnswerBeyondColumnCapacity() throws Exception {
      saveOpenSchedule();
      String tooLong = "가".repeat(ApplicationAnswer.MAX_ANSWER_LENGTH + 1);

      // 막지 않으면 그대로 TEXT 컬럼에 들어가다 500 이 난다 (이슈 #171).
      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(List.of(new ApplicationAnswerRequest(10L, tooLong, null)))))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상한 안이면 질문의 maxLength 를 넘겨도 임시 저장은 된다")
    void stillAcceptsLongDraftWithinCapacity() throws Exception {
      saveOpenSchedule();
      String longButFine = "가".repeat(5_000);

      // 임시 저장은 쓰다 만 상태를 담는 자리다. 질문별 글자 수(기본 300자)는 제출 때 본다.
      mockMvc.perform(put(DRAFT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(List.of(new ApplicationAnswerRequest(10L, longButFine, null)))))
          .andExpect(status().isOk());
    }
  }

  @Nested
  @DisplayName("POST " + SUBMIT_PATH)
  class SubmitApplication {

    @Test
    @DisplayName("제출한다")
    void submits() throws Exception {
      saveOpenSchedule();
      ApplicationQuestion question = applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, 300, null));

      mockMvc.perform(post(SUBMIT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(List.of(
                  new ApplicationAnswerRequest(question.getId(), "지원 동기입니다.", null)))))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
          .andExpect(jsonPath("$.data.submittedAt").exists());
    }

    @Test
    @DisplayName("모집 기간이 아니면 422 다")
    void returns422WhenNotOpen() throws Exception {
      mockMvc.perform(post(SUBMIT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.error.code").value("APPLICATION_NOT_OPEN"));
    }

    @Test
    @DisplayName("필수 질문에 응답하지 않으면 400 이다")
    void returns400WhenRequiredAnswerMissing() throws Exception {
      saveOpenSchedule();
      applicationQuestionRepository.save(ApplicationQuestion.create(
          activeGeneration.getId(), 1, QuestionType.TEXT, "지원 동기", true, 300, null));

      mockMvc.perform(post(SUBMIT_PATH)
              .header("Authorization", guestToken())
              .contentType(MediaType.APPLICATION_JSON)
              .content(draftRequestJson(null)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.error.code").value("REQUIRED_ANSWER_MISSING"));
    }
  }

  @Nested
  @DisplayName("GET " + RESULT_PATH)
  class GetResult {

    @Test
    @DisplayName("제출한 지원서의 결과를 반환한다")
    void returnsResult() throws Exception {
      saveOpenSchedule();
      Application application = applicationRepository.save(Application.createDraft(
          1L, activeGeneration.getId(), "홍길동", "hong@gmail.com", "010-1234-5678", 1L, 11L, 2, null));
      application.submit(LocalDateTime.now());

      mockMvc.perform(get(RESULT_PATH).header("Authorization", guestToken()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
          .andExpect(jsonPath("$.data.statusLabel").value("심사 중"))
          .andExpect(jsonPath("$.data.nextStep").doesNotExist());
    }

    @Test
    @DisplayName("제출한 지원서가 없으면 404 다")
    void returns404WhenNoSubmittedApplication() throws Exception {
      mockMvc.perform(get(RESULT_PATH).header("Authorization", guestToken()))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }
  }
}
