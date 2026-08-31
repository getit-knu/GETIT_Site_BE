package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicantListResult;
import com.getit.domain.recruitment.entity.EvaluationCriterion;
import com.getit.domain.recruitment.entity.EvaluationScore;
import com.getit.domain.recruitment.repository.EvaluationCriterionRepository;
import com.getit.domain.recruitment.repository.EvaluationScoreRepository;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.recruitment.dto.AdjacentApplicantResult;
import com.getit.domain.recruitment.dto.ApplicantDetailResult;
import com.getit.domain.recruitment.dto.ApplicantSummary;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.repository.CollegeRepository;
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

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private EvaluationCriterionRepository evaluationCriterionRepository;

  @Autowired
  private EvaluationScoreRepository evaluationScoreRepository;

  private Generation activeGeneration;

  @BeforeEach
  void setUpActiveGeneration() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    activeGeneration = generationRepository.save(generation);
  }

  private Application draft(Long userId, String name) {
    return draft(userId, name, null);
  }

  private Application draft(Long userId, String name, Long collegeId) {
    return applicationRepository.save(Application.createDraft(
        userId, activeGeneration.getId(), name, name + "@gmail.com", "010-1234-5678",
        collegeId, null, 2, "2021110000"));
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

    /** 기준 둘을 만들고, 한 평가자가 지원서에 매긴 점수를 넣는다. */
    private void score(Application application, Long evaluatorId, int first, int second) {
      List<EvaluationCriterion> criteria =
          evaluationCriterionRepository.findByGenerationId(activeGeneration.getId());
      evaluationScoreRepository.save(EvaluationScore.create(
          application.getId(), criteria.get(0).getId(), evaluatorId, first));
      evaluationScoreRepository.save(EvaluationScore.create(
          application.getId(), criteria.get(1).getId(), evaluatorId, second));
    }

    private void twoCriteria() {
      evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 1, "성실성", "성실성 안내", 50));
      evaluationCriterionRepository.save(
          EvaluationCriterion.create(activeGeneration.getId(), 2, "역량", "역량 안내", 50));
    }

    @Test
    @DisplayName("평가를 끝낸 평가자들의 총점 평균과 인원을 함께 준다")
    void carriesScores() {
      twoCriteria();
      Application applicant = submitted(1L, "김지원");
      score(applicant, 100L, 40, 30);
      score(applicant, 200L, 30, 20);

      ApplicantSummary summary = applicationAdminService
          .listApplicants(null, null, PageRequest.of(0, 20)).applicants().content().get(0);

      // 70 과 50 의 평균. 7.3 상세와 같은 계산이다.
      assertThat(summary.totalScore()).isEqualTo(60.0);
      assertThat(summary.evaluatorCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("일부 기준만 매긴 평가자는 세지 않는다")
    void ignoresIncompleteEvaluators() {
      twoCriteria();
      Application applicant = submitted(1L, "김지원");
      List<EvaluationCriterion> criteria =
          evaluationCriterionRepository.findByGenerationId(activeGeneration.getId());
      evaluationScoreRepository.save(
          EvaluationScore.create(applicant.getId(), criteria.get(0).getId(), 100L, 40));

      ApplicantSummary summary = applicationAdminService
          .listApplicants(null, null, PageRequest.of(0, 20)).applicants().content().get(0);

      // 넣으면 그 사람 총점이 낮게 나와 평균이 실제보다 내려간다.
      assertThat(summary.totalScore()).isNull();
      assertThat(summary.evaluatorCount()).isZero();
    }

    @Test
    @DisplayName("아무도 평가하지 않았으면 점수는 null 이다")
    void nullWhenNotEvaluated() {
      twoCriteria();
      submitted(1L, "김지원");

      ApplicantSummary summary = applicationAdminService
          .listApplicants(null, null, PageRequest.of(0, 20)).applicants().content().get(0);

      // 0 으로 내리면 "0 점을 받았다" 와 구분되지 않는다.
      assertThat(summary.totalScore()).isNull();
    }

    @Test
    @DisplayName("전체 평균은 필터를 걸어도 지원자 전체 기준이다")
    void overviewIgnoresFilter() {
      twoCriteria();
      Application passed = submitted(1L, "김합격");
      Application pending = submitted(2L, "이대기");
      score(passed, 100L, 50, 50);
      score(pending, 100L, 10, 10);
      passed.decideDocumentResult(true);
      applicationRepository.flush();

      ApplicantListResult filtered = applicationAdminService
          .listApplicants(null, ApplicationStatus.DOC_PASS, PageRequest.of(0, 20));

      // 걸러진 집합의 평균이면 필터를 바꿀 때마다 "높은 편" 의 뜻이 달라진다.
      assertThat(filtered.applicants().content()).hasSize(1);
      assertThat(filtered.summary().averageTotalScore()).isEqualTo(60.0);
      assertThat(filtered.summary().evaluatedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("평가된 지원자가 없으면 전체 평균은 null 이다")
    void overviewNullWhenNothingEvaluated() {
      twoCriteria();
      submitted(1L, "김지원");

      ApplicantListResult result =
          applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20));

      assertThat(result.summary().averageTotalScore()).isNull();
      assertThat(result.summary().evaluatedCount()).isZero();
    }

    @Test
    @DisplayName("status 필터가 없으면 DRAFT 를 제외한 활성 기수 지원자를 반환한다")
    void excludesDraftWhenNoStatusFilter() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
        applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)).applicants();

      assertThat(result.content()).extracting(ApplicantSummary::name).containsExactly("홍길동");
      assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("소속 단과대학 이름과 학년을 함께 내려준다")
    void includesCollegeNameAndGrade() {
      College college = collegeRepository.save(College.create("IT융합대학"));
      Application application = draft(1L, "홍길동", college.getId());
      application.submit(LocalDateTime.now());

      PageResponse<ApplicantSummary> result =
        applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)).applicants();

      // 화면에 소속·학년 컬럼이 필요한데 id 만 내려주면 FE 가 채울 방법이 없다 (이슈 #142).
      assertThat(result.content()).singleElement()
          .satisfies(summary -> {
            assertThat(summary.college()).isEqualTo("IT융합대학");
            assertThat(summary.grade()).isEqualTo(2);
          });
    }

    @Test
    @DisplayName("소속을 고르지 않은 지원자는 단과대학이 null 이다")
    void collegeIsNullWhenNotChosen() {
      submitted(1L, "홍길동");

      PageResponse<ApplicantSummary> result =
        applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)).applicants();

      // 임시저장 단계에서는 소속을 비워둘 수 있다. 여기서 터지면 목록 자체가 안 뜬다.
      assertThat(result.content()).singleElement()
          .satisfies(summary -> assertThat(summary.college()).isNull());
    }

    @Test
    @DisplayName("여러 지원자의 소속을 한 번에 조회한다")
    void resolvesCollegeNamesInBatch() {
      College it = collegeRepository.save(College.create("IT융합대학"));
      College biz = collegeRepository.save(College.create("경영대학"));
      draft(1L, "가지원", it.getId()).submit(LocalDateTime.now());
      draft(2L, "나지원", biz.getId()).submit(LocalDateTime.now());
      draft(3L, "다지원", it.getId()).submit(LocalDateTime.now());

      PageResponse<ApplicantSummary> result =
        applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)).applicants();

      // 행마다 조회하면 N+1 이 된다. 중복 제거한 id 로 한 번만 조회한다.
      assertThat(result.content()).extracting(ApplicantSummary::college)
          .containsExactlyInAnyOrder("IT융합대학", "경영대학", "IT융합대학");
    }

    @Test
    @DisplayName("status 필터가 있으면 해당 상태의 지원자만 반환한다")
    void filtersByStatus() {
      submitted(1L, "홍길동");
      draft(2L, "김철수");

      PageResponse<ApplicantSummary> result =
        applicationAdminService.listApplicants(null, ApplicationStatus.DRAFT, PageRequest.of(0, 20)).applicants();

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
        applicationAdminService.listApplicants(null, null, PageRequest.of(0, 20)).applicants();

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
        applicationAdminService.listApplicants(otherGeneration.getId(), null, PageRequest.of(0, 20)).applicants();

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

      PageResponse<ApplicantSummary> result = applicationAdminService
          .listApplicants(null, null, PageRequest.of(0, 20, Sort.by("name")))
          .applicants();

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
