package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.dto.ApplicationPromotionSummary;
import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 다른 도메인이 소비하는 지원서 조회 계약. (작업 분할 계획 4.2, 이슈 #66) */
@SpringBootTest
@Transactional
class ApplicationQueryServiceImplTest {

  @Autowired
  private ApplicationQueryService applicationQueryService;

  @Autowired
  private ApplicationRepository applicationRepository;

  private Application finalPass(Long userId, Long generationId, String name) {
    Application application = applicationRepository.save(Application.createDraft(
        userId, generationId, name, name + "@gmail.com", "010-1234-5678", null, null, 2, "2021110000"));
    application.submit(LocalDateTime.now());
    application.decideDocumentResult(true);
    application.decideFinalResult(true);
    return application;
  }

  @Nested
  @DisplayName("findFinalPassByGenerationId")
  class FindFinalPassByGenerationId {

    @Test
    @DisplayName("같은 기수의 FINAL_PASS 지원서만 반환한다")
    void returnsOnlyFinalPassInGeneration() {
      Application target = finalPass(1L, 9L, "홍길동");
      finalPass(2L, 8L, "다른 기수");

      List<ApplicationPromotionSummary> result = applicationQueryService.findFinalPassByGenerationId(9L);

      assertThat(result).extracting(ApplicationPromotionSummary::applicationId)
          .containsExactly(target.getId());
      assertThat(result.get(0).userId()).isEqualTo(1L);
      assertThat(result.get(0).studentNumber()).isEqualTo("2021110000");
    }

    @Test
    @DisplayName("대상이 없으면 빈 리스트를 반환한다")
    void returnsEmptyWhenNoMatch() {
      assertThat(applicationQueryService.findFinalPassByGenerationId(999L)).isEmpty();
    }
  }

  @Nested
  @DisplayName("findFinalPassByIdsAndGenerationId")
  class FindFinalPassByIdsAndGenerationId {

    @Test
    @DisplayName("id · 기수 · FINAL_PASS 상태가 모두 일치하는 것만 반환한다")
    void filtersByAllConditions() {
      Application target = finalPass(1L, 9L, "홍길동");
      Application otherGeneration = finalPass(2L, 8L, "다른 기수");

      List<ApplicationPromotionSummary> result = applicationQueryService.findFinalPassByIdsAndGenerationId(
          List.of(target.getId(), otherGeneration.getId(), 999L), 9L);

      assertThat(result).extracting(ApplicationPromotionSummary::applicationId)
          .containsExactly(target.getId());
    }
  }

  @Nested
  @DisplayName("countSubmittedByGenerationId")
  class CountSubmittedByGenerationId {

    @Test
    @DisplayName("같은 기수의 DRAFT 를 제외한 지원서 수를 센다")
    void countsExcludingDraft() {
      finalPass(1L, 9L, "홍길동");
      applicationRepository.save(Application.createDraft(
          2L, 9L, "임시저장", "draft@gmail.com", "010-1234-5678", null, null, 2, "2021110001"));
      finalPass(3L, 8L, "다른 기수");

      assertThat(applicationQueryService.countSubmittedByGenerationId(9L)).isEqualTo(1);
    }

    @Test
    @DisplayName("대상이 없으면 0이다")
    void returnsZeroWhenNoMatch() {
      assertThat(applicationQueryService.countSubmittedByGenerationId(999L)).isEqualTo(0);
    }
  }
}
