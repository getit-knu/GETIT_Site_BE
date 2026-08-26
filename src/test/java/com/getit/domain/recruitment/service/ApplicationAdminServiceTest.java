package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.AdjacentApplicantResult;
import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.exception.RecruitmentErrorCode;
import com.getit.domain.recruitment.repository.ApplicationAnswerRepository;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ApplicationAdminServiceTest {

  @Autowired
  private ApplicationAdminService applicationAdminService;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  @Autowired
  private GenerationRepository generationRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
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
  @DisplayName("listApplicants")
  class ListApplicants {

    @Test
    @DisplayName("status 필터가 없으면 DRAFT 를 제외한 활성 기수 지원자를 반환한다")
    void excludesDraftWhenNoStatusFilter() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("홍길동");
      assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("status 필터가 있으면 해당 상태의 지원자만 반환한다")
    void filtersByStatus() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, ApplicationStatus.DRAFT, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("김철수");
    }

    @Test
    @DisplayName("generationId 가 없으면 활성 기수만 조회하고 다른 기수는 포함하지 않는다")
    void excludesOtherGenerationWhenNoGenerationIdGiven() {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난 기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("홍길동");
    }

    @Test
    @DisplayName("generationId 를 지정하면 해당 기수(비활성 포함) 지원자를 조회한다")
    void filtersByExplicitGenerationId() {
      submitted(1L, "홍길동");
      Generation otherGeneration = generationRepository.save(Generation.create(8, 2026));
      Application otherGenApplication = applicationRepository.save(Application.createDraft(
          2L, otherGeneration.getId(), "지난 기수", "old@gmail.com", "010-0000-0000", null, null, 2, null));
      otherGenApplication.submit(LocalDateTime.now());

      PageResponse<ApplicantSummary> result =
          applicationAdminService.listApplicants(otherGeneration.getId(), null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("지난 기수");
    }

    @Test
    @DisplayName("generationId 가 없고 활성 기수도 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }

    @Test
    @DisplayName("클라이언트가 보낸 정렬은 무시하고 항상 제출일시 내림차순 + id 로 정렬한다")
    void ignoresClientSuppliedSort() {
      // 이름순(ASC)이면 "가나다"(lowerId)가 먼저 나와야 하지만, 강제된 정렬(제출일시 동률 -> id
      // 내림차순)이면 higherId 가 먼저 나온다 — 두 결과가 서로 달라야 강제가 실제로 적용됐는지
      // 구분할 수 있다.
      LocalDateTime tie = LocalDateTime.of(2026, 9, 1, 11, 0);
      Application lowerId = submittedAt(1L, "가나다", tie);
      Application higherId = submittedAt(2L, "다바가", tie);

      PageResponse<ApplicantSummary> result = applicationAdminService.listApplicants(
          null, null, PageRequest.of(0, 20, Sort.by("name")));

      assertThat(result.content()).extracting(ApplicantSummary::id)
          .containsExactly(higherId.getId(), lowerId.getId());
    }
  }

  @Nested
  @DisplayName("getApplicantDetail")
  class GetApplicantDetail {

    @Test
    @DisplayName("기본 정보와 답변을 함께 반환한다")
    void returnsDetailWithAnswers() {
      Application application = submitted(1L, "홍길동");
      applicationAnswerRepository.save(
          ApplicationAnswer.create(application.getId(), 10L, "지원 동기입니다.", null));

      ApplicantDetailResult result = applicationAdminService.getApplicantDetail(application.getId());

      assertThat(result.id()).isEqualTo(application.getId());
      assertThat(result.status()).isEqualTo(ApplicationStatus.SUBMITTED);
      assertThat(result.basicInfo().name()).isEqualTo("홍길동");
      assertThat(result.basicInfo().studentNumber()).isEqualTo("2021110000");
      assertThat(result.answers()).hasSize(1);
      assertThat(result.submittedAt()).isNotNull();
    }

    @Test
    @DisplayName("지난 기수 지원자도 조회할 수 있다")
    void returnsDetailForInactiveGeneration() {
      Application application = submitted(1L, "홍길동");
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThat(applicationAdminService.getApplicantDetail(application.getId()).id())
          .isEqualTo(application.getId());
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationAdminService.getApplicantDetail(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 조회할 수 없다")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");

      assertThatThrownBy(() -> applicationAdminService.getApplicantDetail(application.getId()))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("getAdjacentApplicants")
  class GetAdjacentApplicants {

    @Test
    @DisplayName("제출일시 내림차순 기준으로 이전 · 다음 지원서 id 를 반환한다")
    void returnsPreviousAndNext() {
      Application oldest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 0));
      Application middle = submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 11, 0));
      Application newest = submittedAt(3L, "이영희", LocalDateTime.of(2026, 9, 1, 12, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(middle.getId(), null, null);

      assertThat(result.previousApplicationId()).isEqualTo(newest.getId());
      assertThat(result.nextApplicationId()).isEqualTo(oldest.getId());
    }

    @Test
    @DisplayName("submittedAt 이 같으면 id 내림차순으로 tie-break 한다")
    void tieBreaksById() {
      LocalDateTime tie = LocalDateTime.of(2026, 9, 1, 11, 0);
      Application oldest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 0));
      Application lowerId = submittedAt(2L, "김철수", tie);
      Application higherId = submittedAt(3L, "이영희", tie);

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(lowerId.getId(), null, null);

      assertThat(result.previousApplicationId()).isEqualTo(higherId.getId());
      assertThat(result.nextApplicationId()).isEqualTo(oldest.getId());
    }

    @Test
    @DisplayName("가장 최근(맨 앞) 지원서는 이전이 없다")
    void firstHasNoPrevious() {
      Application newest = submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 12, 0));
      submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 10, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(newest.getId(), null, null);

      assertThat(result.previousApplicationId()).isNull();
      assertThat(result.nextApplicationId()).isNotNull();
    }

    @Test
    @DisplayName("가장 오래된(맨 뒤) 지원서는 다음이 없다")
    void lastHasNoNext() {
      submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 12, 0));
      Application oldest = submittedAt(2L, "김철수", LocalDateTime.of(2026, 9, 1, 10, 0));

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(oldest.getId(), null, null);

      assertThat(result.previousApplicationId()).isNotNull();
      assertThat(result.nextApplicationId()).isNull();
    }

    @Test
    @DisplayName("현재 지원서가 status 필터에 안 맞으면 이전 · 다음 모두 null 이다")
    void bothNullWhenStatusFilterDoesNotMatch() {
      Application application = submitted(1L, "홍길동");

      AdjacentApplicantResult result =
          applicationAdminService.getAdjacentApplicants(application.getId(), null, ApplicationStatus.DOC_PASS);

      assertThat(result.previousApplicationId()).isNull();
      assertThat(result.nextApplicationId()).isNull();
    }

    @Test
    @DisplayName("DRAFT 상태의 지원서는 조회할 수 없다")
    void throwsWhenDraft() {
      Application application = draft(1L, "홍길동");

      assertThatThrownBy(() -> applicationAdminService.getAdjacentApplicants(application.getId(), null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 지원서면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> applicationAdminService.getAdjacentApplicants(999L, null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.APPLICATION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("exportApplicantsExcel")
  class ExportApplicantsExcel {

    @Test
    @DisplayName("DRAFT 를 제외한 지원자를 이름 · 학번 · 한글 상태 라벨 · 포맷된 제출일시로 내보낸다")
    void exportsApplicantsExcludingDraft() throws IOException {
      // LocalDateTime.now() 는 초가 0이면 toString()에서 초가 생략돼 포맷 비교가 흔들릴 수 있다
      // (CLAUDE.md 트러블슈팅 노트 참고) — 고정된 시각을 쓴다.
      submittedAt(1L, "홍길동", LocalDateTime.of(2026, 9, 1, 10, 30, 5));
      draft(2L, "김철수");

      byte[] excel = applicationAdminService.exportApplicantsExcel(null, null);

      try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(excel))) {
        Sheet sheet = workbook.getSheetAt(0);
        assertThat(sheet.getLastRowNum()).isEqualTo(1);
        Row dataRow = sheet.getRow(1);
        assertThat(dataRow.getCell(0).getStringCellValue()).isEqualTo("홍길동");
        assertThat(dataRow.getCell(1).getStringCellValue()).isEqualTo("2021110000");
        assertThat(dataRow.getCell(2).getStringCellValue()).isEqualTo("심사 중");
        assertThat(dataRow.getCell(3).getStringCellValue()).isEqualTo("2026-09-01 10:30:05");
      }
    }

    @Test
    @DisplayName("활성 기수가 없고 generationId 도 없으면 예외가 발생한다")
    void throwsWhenNoActiveGeneration() {
      activeGeneration.deactivate();
      generationRepository.flush();

      assertThatThrownBy(() -> applicationAdminService.exportApplicantsExcel(null, null))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(RecruitmentErrorCode.ACTIVE_GENERATION_NOT_FOUND);
    }
  }
}
