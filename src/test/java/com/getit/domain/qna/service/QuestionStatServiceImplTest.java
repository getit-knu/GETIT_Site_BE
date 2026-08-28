package com.getit.domain.qna.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuestionStatServiceImplTest {

  @Autowired
  private QuestionStatService questionStatService;

  @Autowired
  private QuestionRepository questionRepository;

  @Test
  @DisplayName("countUnanswered 는 PENDING 만 센다")
  void countsPendingOnly() {
    questionRepository.save(Question.create(1L, 10L, "대기1"));
    questionRepository.save(Question.create(1L, 10L, "대기2"));
    Question answered = questionRepository.save(Question.create(1L, 10L, "답변됨"));
    answered.markAnswered();
    questionRepository.flush();

    assertThat(questionStatService.countUnanswered()).isEqualTo(2);
  }

  @Test
  @DisplayName("findRecent 는 미답변만 최신순으로 size 개 반환한다")
  void returnsRecentPendingLimited() {
    questionRepository.save(Question.create(1L, 10L, "old"));
    questionRepository.save(Question.create(1L, 10L, "mid"));
    questionRepository.save(Question.create(1L, 10L, "new"));
    Question answered = questionRepository.save(Question.create(1L, 10L, "answered"));
    answered.markAnswered();
    questionRepository.flush();

    var recent = questionStatService.findRecent(2);

    assertThat(recent).extracting(RecentQuestion::content).containsExactly("new", "mid");
  }

  @Test
  @DisplayName("size 가 0 이하면 빈 리스트다")
  void emptyWhenSizeNotPositive() {
    questionRepository.save(Question.create(1L, 10L, "q"));

    assertThat(questionStatService.findRecent(0)).isEmpty();
  }
}
