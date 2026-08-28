package com.getit.domain.qna.service;

import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.repository.QuestionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionStatServiceImpl implements QuestionStatService {

  private final QuestionRepository questionRepository;

  @Override
  public long countUnanswered() {
    return questionRepository.countByStatus(QnaStatus.PENDING);
  }

  @Override
  public List<RecentQuestion> findRecent(int size) {
    if (size <= 0) {
      return List.of();
    }
    return questionRepository.findByOrderByCreatedAtDescIdDesc(PageRequest.of(0, size)).stream()
        .map(question -> new RecentQuestion(
            question.getId(), question.getAuthorId(), question.getContent(),
            question.getCreatedAt(), question.getLectureId()))
        .toList();
  }
}
