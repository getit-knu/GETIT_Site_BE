package com.getit.domain.recruitment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.ApplicationQuestion;
import com.getit.domain.recruitment.entity.QuestionType;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class ApplicationQuestionRepositoryTest {

  @Autowired
  private ApplicationQuestionRepository applicationQuestionRepository;

  private ApplicationQuestion question(Long generationId, int order, String content) {
    return ApplicationQuestion.create(generationId, order, QuestionType.TEXT, content, true, 300, null);
  }

  @Test
  @DisplayName("기수별로 order 오름차순 조회한다")
  void findsByGenerationIdOrderedByOrder() {
    applicationQuestionRepository.save(question(1L, 2, "두번째"));
    applicationQuestionRepository.save(question(1L, 1, "첫번째"));
    applicationQuestionRepository.save(question(2L, 1, "다른 기수"));

    assertThat(applicationQuestionRepository.findByGenerationId(1L))
        .extracting(ApplicationQuestion::getContent)
        .containsExactly("첫번째", "두번째");
  }

  @Test
  @DisplayName("질문이 없는 기수는 빈 리스트를 반환한다")
  void returnsEmptyWhenNoQuestions() {
    assertThat(applicationQuestionRepository.findByGenerationId(999L)).isEmpty();
  }

  @Test
  @DisplayName("기수별 질문 개수를 센다")
  void countsByGenerationId() {
    applicationQuestionRepository.save(question(1L, 1, "첫번째"));
    applicationQuestionRepository.save(question(1L, 2, "두번째"));
    applicationQuestionRepository.save(question(2L, 1, "다른 기수"));

    assertThat(applicationQuestionRepository.countByGenerationId(1L)).isEqualTo(2);
    assertThat(applicationQuestionRepository.countByGenerationId(999L)).isZero();
  }

  @Test
  @DisplayName("id 와 기수가 모두 일치해야 조회된다")
  void findsByIdAndGenerationIdOnlyWhenBothMatch() {
    ApplicationQuestion saved = applicationQuestionRepository.save(question(1L, 1, "첫번째"));

    assertThat(applicationQuestionRepository.findByIdAndGenerationId(saved.getId(), 1L)).isPresent();
    assertThat(applicationQuestionRepository.findByIdAndGenerationId(saved.getId(), 2L)).isEmpty();
  }
}
