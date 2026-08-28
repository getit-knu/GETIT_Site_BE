package com.getit.domain.qna.service;

import com.getit.domain.lecture.service.LectureAccessService;
import com.getit.domain.qna.dto.MemberQuestionRequest;
import com.getit.domain.qna.dto.MemberQuestionResult;
import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.AnswerRepository;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.qna.util.QnaDateTimes;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionService {

  private final QuestionRepository questionRepository;
  private final AnswerRepository answerRepository;
  private final LectureAccessService lectureAccessService;
  private final UserAccountService userAccountService;

  @Transactional
  public MemberQuestionResult.CreateResult create(
      Long lectureId, MemberQuestionRequest.Create request, Long userId) {
    lectureAccessService.requireVisibleToMember(lectureId, userId);
    Question saved = questionRepository.save(Question.create(userId, lectureId, request.content()));
    return MemberQuestionResult.CreateResult.from(saved);
  }

  public List<MemberQuestionResult.ListItem> getMyQuestions(Long lectureId, Long userId) {
    lectureAccessService.requireVisibleToMember(lectureId, userId);
    List<Question> questions =
        questionRepository.findByLectureIdAndAuthorIdOrderByCreatedAtDescIdDesc(lectureId, userId);
    if (questions.isEmpty()) {
      return List.of();
    }
    Map<Long, Answer> answerByQuestionId = answerRepository
        .findAllByQuestionIdIn(questions.stream().map(Question::getId).toList()).stream()
        .collect(Collectors.toMap(Answer::getQuestionId, Function.identity()));
    String authorName = resolveName(userId);
    Map<Long, String> adminNameById = resolveNames(answerByQuestionId.values());

    return questions.stream()
        .map(question -> {
          Answer answer = answerByQuestionId.get(question.getId());
          List<MemberQuestionResult.AnswerItem> answers = answer == null
              ? List.of()
              : List.of(MemberQuestionResult.AnswerItem.of(
                  answer, adminNameById.getOrDefault(answer.getAdminId(), "UNKNOWN")));
          return new MemberQuestionResult.ListItem(
              question.getId(), authorName, question.getContent(),
              QnaDateTimes.toOffset(question.getCreatedAt()), question.getStatus(), answers);
        })
        .toList();
  }

  private String resolveName(long userId) {
    return userAccountService.findActiveById(userId).map(UserAccount::name).orElse("UNKNOWN");
  }

  // adminId 별 이름을 개별 조회한다. user 도메인에 findActiveByIds(#28 협의) 가 생기면 일괄 조회로 교체.
  private Map<Long, String> resolveNames(Collection<Answer> answers) {
    return answers.stream()
        .map(Answer::getAdminId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), this::resolveName));
  }
}
