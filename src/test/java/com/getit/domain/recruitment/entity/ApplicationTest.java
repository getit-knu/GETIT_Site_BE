package com.getit.domain.recruitment.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApplicationTest {

  @Test
  @DisplayName("임시 저장으로 생성하면 상태가 DRAFT 다")
  void createDraftStartsAsDraft() {
    Application application = Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2);

    assertThat(application.getUserId()).isEqualTo(1L);
    assertThat(application.getGenerationId()).isEqualTo(9L);
    assertThat(application.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
    assertThat(application.getName()).isEqualTo("홍길동");
    assertThat(application.getEmail()).isEqualTo("hong@gmail.com");
    assertThat(application.getPhoneNumber()).isEqualTo("010-1234-5678");
    assertThat(application.getCollegeId()).isNull();
    assertThat(application.getMajorId()).isNull();
    assertThat(application.getGrade()).isEqualTo(2);
    assertThat(application.getSubmittedAt()).isNull();
  }
}
