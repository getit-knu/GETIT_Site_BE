package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.global.config.JpaAuditingConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

  @Nested
  @DisplayName("updateStatusIfCurrentStatus (7.4)")
  class UpdateStatusIfCurrentStatus {

    @Test
    @DisplayName("현재 상태가 requiredStatus 와 일치하면 갱신하고 1을 반환한다")
    void updatesWhenStatusMatches() {
      Application application = submitted(1L, 9L, "홍길동");

      int updated = applicationRepository.updateStatusIfCurrentStatus(
          application.getId(), ApplicationStatus.DOC_PASS, ApplicationStatus.SUBMITTED);

      assertThat(updated).isEqualTo(1);
      assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
          .isEqualTo(ApplicationStatus.DOC_PASS);
    }

    @Test
    @DisplayName("현재 상태가 requiredStatus 와 다르면 갱신하지 않고 0을 반환한다 (동시 결정 방지)")
    void doesNotUpdateWhenStatusDiffers() {
      Application application = submitted(1L, 9L, "홍길동");
      applicationRepository.updateStatusIfCurrentStatus(
          application.getId(), ApplicationStatus.DOC_PASS, ApplicationStatus.SUBMITTED);

      int secondUpdate = applicationRepository.updateStatusIfCurrentStatus(
          application.getId(), ApplicationStatus.DOC_FAIL, ApplicationStatus.SUBMITTED);

      assertThat(secondUpdate).isZero();
      assertThat(applicationRepository.findById(application.getId()).orElseThrow().getStatus())
          .isEqualTo(ApplicationStatus.DOC_PASS);
    }

    @Test
    @DisplayName("없는 id 면 0을 반환한다")
    void returnsZeroWhenNotFound() {
      int updated = applicationRepository.updateStatusIfCurrentStatus(
          999L, ApplicationStatus.DOC_PASS, ApplicationStatus.SUBMITTED);

      assertThat(updated).isZero();
    }
  }

  @Nested
  @DisplayName("findByGenerationIdAndStatus (7.6, Sort)")
  class FindByGenerationIdAndStatusSorted {

    @Test
    @DisplayName("페이징 없이 정렬 기준대로 전체 조회한다")
    void findsAllMatchingSorted() {
      Application first = draft(1L, 9L, "홍길동");
      first.submit(LocalDateTime.of(2026, 9, 1, 10, 0));
      Application second = draft(2L, 9L, "김철수");
      second.submit(LocalDateTime.of(2026, 9, 1, 11, 0));
      draft(3L, 9L, "이영희");
      submitted(4L, 8L, "다른 기수");

      List<Application> result = applicationRepository.findByGenerationIdAndStatus(
          9L, ApplicationStatus.SUBMITTED, Sort.by(Sort.Direction.DESC, "submittedAt"));

      assertThat(result).extracting(Application::getName).containsExactly("김철수", "홍길동");
    }
  }

  @Nested
  @DisplayName("findByGenerationIdAndStatusNot (7.6, Sort)")
  class FindByGenerationIdAndStatusNotSorted {

    @Test
    @DisplayName("DRAFT 를 제외하고 정렬 기준대로 전체 조회한다")
    void excludesDraftSorted() {
      submitted(1L, 9L, "홍길동");
      draft(2L, 9L, "김철수");
      submitted(3L, 8L, "다른 기수");

      List<Application> result = applicationRepository.findByGenerationIdAndStatusNot(
          9L, ApplicationStatus.DRAFT, Sort.by(Sort.Direction.DESC, "id"));

      assertThat(result).extracting(Application::getName).containsExactly("홍길동");
    }
  }

  @Nested
  @DisplayName("findNextIdByGenerationIdAndStatus · findPreviousIdByGenerationIdAndStatus (7.5)")
  class FindAdjacentIds {

    @Test
    @DisplayName("다음은 정렬 기준(submittedAt desc, id desc)에서 현재 바로 뒤 id 를 반환한다")
    void findsNextId() {
      Application oldest = draft(1L, 9L, "홍길동");
      oldest.submit(LocalDateTime.of(2026, 9, 1, 10, 0));
      Application middle = draft(2L, 9L, "김철수");
      middle.submit(LocalDateTime.of(2026, 9, 1, 11, 0));

      List<Long> next = applicationRepository.findNextIdByGenerationIdAndStatus(
          9L, ApplicationStatus.SUBMITTED, middle.getSubmittedAt(), middle.getId(), PageRequest.of(0, 1));

      assertThat(next).containsExactly(oldest.getId());
    }

    @Test
    @DisplayName("이전은 정렬 기준에서 현재 바로 앞 id 를 반환한다")
    void findsPreviousId() {
      Application middle = draft(1L, 9L, "홍길동");
      middle.submit(LocalDateTime.of(2026, 9, 1, 11, 0));
      Application newest = draft(2L, 9L, "김철수");
      newest.submit(LocalDateTime.of(2026, 9, 1, 12, 0));

      List<Long> previous = applicationRepository.findPreviousIdByGenerationIdAndStatus(
          9L, ApplicationStatus.SUBMITTED, middle.getSubmittedAt(), middle.getId(), PageRequest.of(0, 1));

      assertThat(previous).containsExactly(newest.getId());
    }

    @Test
    @DisplayName("submittedAt 이 같으면 id 로 tie-break 한다")
    void tieBreaksById() {
      LocalDateTime tie = LocalDateTime.of(2026, 9, 1, 11, 0);
      Application lowerId = draft(1L, 9L, "홍길동");
      lowerId.submit(tie);
      Application higherId = draft(2L, 9L, "김철수");
      higherId.submit(tie);

      List<Long> next = applicationRepository.findNextIdByGenerationIdAndStatus(
          9L, ApplicationStatus.SUBMITTED, higherId.getSubmittedAt(), higherId.getId(), PageRequest.of(0, 1));

      assertThat(next).containsExactly(lowerId.getId());
    }

    @Test
    @DisplayName("더 이상 없으면 빈 리스트를 반환한다")
    void returnsEmptyWhenNoMore() {
      Application only = draft(1L, 9L, "홍길동");
      only.submit(LocalDateTime.of(2026, 9, 1, 10, 0));

      List<Long> next = applicationRepository.findNextIdByGenerationIdAndStatus(
          9L, ApplicationStatus.SUBMITTED, only.getSubmittedAt(), only.getId(), PageRequest.of(0, 1));

      assertThat(next).isEmpty();
    }
  }
}
