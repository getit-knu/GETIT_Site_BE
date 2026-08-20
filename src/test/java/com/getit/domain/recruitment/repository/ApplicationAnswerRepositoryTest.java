package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.ApplicationAnswer;
import com.getit.global.config.JpaAuditingConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ApplicationAnswerRepositoryTest {

  @Autowired
  private ApplicationAnswerRepository applicationAnswerRepository;

  @Test
  @DisplayName("지원서 id 로 답변 전체를 조회한다")
  void findsByApplicationId() {
    applicationAnswerRepository.save(ApplicationAnswer.create(1L, 10L, "지원 동기입니다.", null));
    applicationAnswerRepository.save(ApplicationAnswer.create(1L, 30L, null, List.of("sw")));
    applicationAnswerRepository.save(ApplicationAnswer.create(2L, 10L, "다른 지원서 답변", null));

    assertThat(applicationAnswerRepository.findByApplicationId(1L))
        .extracting(ApplicationAnswer::getQuestionId)
        .containsExactlyInAnyOrder(10L, 30L);
  }

  @Test
  @DisplayName("답변이 없는 지원서는 빈 리스트를 반환한다")
  void returnsEmptyWhenNoAnswers() {
    assertThat(applicationAnswerRepository.findByApplicationId(999L)).isEmpty();
  }

  @Test
  @DisplayName("applicationId 와 questionId 가 모두 일치해야 조회된다")
  void findsByApplicationIdAndQuestionIdOnlyWhenBothMatch() {
    applicationAnswerRepository.save(ApplicationAnswer.create(1L, 10L, "지원 동기입니다.", null));

    assertThat(applicationAnswerRepository.findByApplicationIdAndQuestionId(1L, 10L)).isPresent();
    assertThat(applicationAnswerRepository.findByApplicationIdAndQuestionId(1L, 999L)).isEmpty();
    assertThat(applicationAnswerRepository.findByApplicationIdAndQuestionId(999L, 10L)).isEmpty();
  }
}
