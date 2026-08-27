package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationTest {

  @Test
  @DisplayName("임시 저장으로 생성하면 상태가 DRAFT 다")
  void createDraftStartsAsDraft() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");

    assertThat(application.getUserId()).isEqualTo(1L);
    assertThat(application.getGenerationId()).isEqualTo(9L);
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    assertThat(application.getName()).isEqualTo("홍길동");
    assertThat(application.getEmail()).isEqualTo("hong@gmail.com");
    assertThat(application.getPhoneNumber()).isEqualTo("010-1234-5678");
    assertThat(application.getCollegeId()).isNull();
    assertThat(application.getMajorId()).isNull();
    assertThat(application.getGrade()).isEqualTo(2);
    assertThat(application.getStudentNumber()).isEqualTo("2021110000");
    assertThat(application.getSubmittedAt()).isNull();
  }

  @Test
  @DisplayName("updateDraft 는 기본 정보를 덮어쓰고 상태·userId·generationId 는 그대로 둔다")
  void updateDraftOverwritesBasicInfo() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");

    application.updateDraft(
        "홍길동", "hong2@gmail.com", "010-9999-8888", 1L, 11L, 3, "2021110001");

    assertThat(application.getUserId()).isEqualTo(1L);
    assertThat(application.getGenerationId()).isEqualTo(9L);
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    assertThat(application.getEmail()).isEqualTo("hong2@gmail.com");
    assertThat(application.getPhoneNumber()).isEqualTo("010-9999-8888");
    assertThat(application.getCollegeId()).isEqualTo(1L);
    assertThat(application.getMajorId()).isEqualTo(11L);
    assertThat(application.getGrade()).isEqualTo(3);
    assertThat(application.getStudentNumber()).isEqualTo("2021110001");
  }

  @Test
  @DisplayName("submit 은 상태를 SUBMITTED 로 바꾸고 submittedAt 을 채운다")
  void submitChangesStatusAndSetsSubmittedAt() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");
    LocalDateTime submittedAt = LocalDateTime.of(2026, 9, 8, 21, 3, 44);

    application.submit(submittedAt);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
    assertThat(application.getSubmittedAt()).isEqualTo(submittedAt);
  }

  @Test
  @DisplayName("decideDocumentResult(true) 는 상태를 DOC_PASS 로 바꾼다")
  void decideDocumentResultTruePassesToDocPass() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");
    application.submit(LocalDateTime.now());

    application.decideDocumentResult(true);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DOC_PASS);
  }

  @Test
  @DisplayName("decideDocumentResult(false) 는 상태를 DOC_FAIL 로 바꾼다")
  void decideDocumentResultFalseFailsToDocFail() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");
    application.submit(LocalDateTime.now());

    application.decideDocumentResult(false);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DOC_FAIL);
  }

  @Test
  @DisplayName("decideFinalResult(true) 는 상태를 FINAL_PASS 로 바꾼다")
  void decideFinalResultTruePassesToFinalPass() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");
    application.submit(LocalDateTime.now());
    application.decideDocumentResult(true);

    application.decideFinalResult(true);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.FINAL_PASS);
  }

  @Test
  @DisplayName("decideFinalResult(false) 는 상태를 FINAL_FAIL 로 바꾼다")
  void decideFinalResultFalseFailsToFinalFail() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000");
    application.submit(LocalDateTime.now());
    application.decideDocumentResult(true);

    application.decideFinalResult(false);

    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.FINAL_FAIL);
  }
}
