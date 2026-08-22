package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ApplicationRepositoryTest {

  @Autowired
  private ApplicationRepository applicationRepository;

  private Application draft(Long userId, Long generationId, String name) {
    return applicationRepository.save(Application.createDraft(
        userId, generationId, name, name + "@gmail.com", "010-1234-5678", null, null, 2, null));
  }

  private Application submitted(Long userId, Long generationId, String name) {
    Application application = draft(userId, generationId, name);
    application.submit(LocalDateTime.now());
    return application;
  }

  @Test
  @DisplayName("userId 와 generationId 가 모두 일치해야 조회된다")
  void findsByUserIdAndGenerationIdOnlyWhenBothMatch() {
    applicationRepository.save(Application.createDraft(
        1L, 9L, "홍길동", "hong@gmail.com", "010-1234-5678", null, null, 2, "2021110000"));

    assertThat(applicationRepository.findByUserIdAndGenerationId(1L, 9L)).isPresent();
    assertThat(applicationRepository.findByUserIdAndGenerationId(1L, 8L)).isEmpty();
    assertThat(applicationRepository.findByUserIdAndGenerationId(2L, 9L)).isEmpty();
  }

  @Test
  @DisplayName("지원서가 없으면 빈 값을 반환한다")
  void returnsEmptyWhenNoApplication() {
    assertThat(applicationRepository.findByUserIdAndGenerationId(999L, 999L)).isEmpty();
  }

  @Nested
  @DisplayName("findByGenerationIdAndStatus (7.1)")
  class FindByGenerationIdAndStatus {

    @Test
    @DisplayName("기수와 상태가 모두 일치하는 지원서만 페이징 조회한다")
    void findsMatchingApplicationsPaged() {
      submitted(1L, 9L, "홍길동");
      submitted(2L, 9L, "김철수");
      draft(3L, 9L, "이영희");
      submitted(4L, 8L, "다른 기수");

      Page<Application> page =
          applicationRepository.findByGenerationIdAndStatus(9L, ApplicationStatus.SUBMITTED, PageRequest.of(0, 1));

      assertThat(page.getTotalElements()).isEqualTo(2);
      assertThat(page.getTotalPages()).isEqualTo(2);
      assertThat(page.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("일치하는 지원서가 없으면 빈 페이지를 반환한다")
    void returnsEmptyPageWhenNoMatch() {
      Page<Application> page =
          applicationRepository.findByGenerationIdAndStatus(999L, ApplicationStatus.SUBMITTED, PageRequest.of(0, 20));

      assertThat(page.getTotalElements()).isZero();
      assertThat(page.getContent()).isEmpty();
    }
  }

  @Nested
  @DisplayName("findByGenerationIdAndStatusNot (7.1)")
  class FindByGenerationIdAndStatusNot {

    @Test
    @DisplayName("DRAFT 를 제외한 같은 기수의 지원서를 조회한다")
    void excludesDraft() {
      submitted(1L, 9L, "홍길동");
      draft(2L, 9L, "김철수");
      submitted(3L, 8L, "다른 기수");

      Page<Application> page = applicationRepository.findByGenerationIdAndStatusNot(
          9L, ApplicationStatus.DRAFT, PageRequest.of(0, 20));

      assertThat(page.getContent()).extracting(Application::getName).containsExactly("홍길동");
    }
  }
}
