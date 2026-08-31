package com.getit.domain.qna.service;

import com.getit.domain.lecture.service.LectureAccessService;
import com.getit.domain.qna.dto.MemberQuestionRequest;
import com.getit.domain.qna.dto.MemberQuestionResult;
import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.AnswerRepository;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.qna.util.QnaDateTimes;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.Objects;
import com.getit.global.dto.PageResponse;
import com.getit.domain.lecture.service.LectureQueryService;
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
  private final LectureQueryService lectureQueryService;

  @Transactional
  public MemberQuestionResult.CreateResult create(
      Long lectureId, MemberQuestionRequest.Create request, Long userId) {
    lectureAccessService.requireVisibleToMember(lectureId, userId);
    Question saved = questionRepository.save(Question.create(userId, lectureId, request.content()));
    return MemberQuestionResult.CreateResult.from(saved);
  }

  /**
   * 내가 쓴 질문 전체. 강의를 가로지른다. (이슈 #185)
   *
   * <p>강의별 조회(4.6)와 같은 규칙이다 — 내 질문만 돌려준다. 다른 점은 강의에 매이지 않고,
   * 페이징 · 최신순이 서버에서 정해지며, 응답에 강의 정보가 실린다는 것뿐이다.
   *
   * <p>강의 접근 권한은 보지 않는다. 내가 쓴 질문이고, 이미 쓸 때 확인했다. 나중에 강의가
   * 비공개로 바뀌어도 내가 남긴 글은 내 목록에서 사라지지 않아야 한다.
   */
  public PageResponse<MemberQuestionResult.MyListItem> getMyQuestions(
      Long userId, QnaStatus status, Pageable pageable
  ) {
    Page<Question> questions = questionRepository.findMyQuestions(userId, status, pageable);
    if (questions.isEmpty()) {
      // 빈 목록에 답변·강의 조회를 더 보내지 않는다. 매퍼는 호출되지 않는다.
      return PageResponse.from(questions, question -> null);
    }

    Map<Long, Answer> answerByQuestionId = answerRepository
        .findAllByQuestionIdIn(questions.getContent().stream().map(Question::getId).toList()).stream()
        .collect(Collectors.toMap(Answer::getQuestionId, Function.identity()));
    Map<Long, String> adminNameById = resolveNames(answerByQuestionId.values());
    Map<Long, String> lectureTitleById = resolveLectureTitles(questions.getContent());

    return PageResponse.from(questions, question -> {
      Answer answer = answerByQuestionId.get(question.getId());
      List<MemberQuestionResult.AnswerItem> answers = answer == null
          ? List.of()
          : List.of(MemberQuestionResult.AnswerItem.of(
              answer, adminNameById.getOrDefault(answer.getAdminId(), "UNKNOWN")));
      return new MemberQuestionResult.MyListItem(
          question.getId(),
          question.getLectureId(),
          question.getLectureId() == null ? null : lectureTitleById.get(question.getLectureId()),
          question.getContent(),
          QnaDateTimes.toOffset(question.getCreatedAt()),
          question.getStatus(),
          answers);
    });
  }

  /** 목록 전체의 강의 제목을 한 번에 읽는다. 줄마다 조회하면 N+1 이 된다. */
  private Map<Long, String> resolveLectureTitles(List<Question> questions) {
    List<Long> lectureIds = questions.stream()
        .map(Question::getLectureId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    return lectureIds.isEmpty() ? Map.of() : lectureQueryService.findTitlesByIds(lectureIds);
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
