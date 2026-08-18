package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.Application;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ApplicationRepositoryTest {

  @Autowired
  private ApplicationRepository applicationRepository;

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
}
