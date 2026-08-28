package com.getit.domain.qna.admin.service;

import com.getit.domain.lecture.service.LectureQueryService;
import com.getit.domain.qna.admin.dto.AdminAnswerRequest;
import com.getit.domain.qna.admin.dto.AdminAnswerResult;
import com.getit.domain.qna.admin.dto.AdminQuestionResult;
import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.exception.QnaErrorCode;
import com.getit.domain.qna.repository.AnswerRepository;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.qna.util.QnaDateTimes;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.domain.user.service.UserQueryService;
import com.getit.global.dto.PageResponse;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class QuestionAdminService {

  private final QuestionRepository questionRepository;
  private final AnswerRepository answerRepository;
  private final LectureQueryService lectureQueryService;
  private final UserAccountService userAccountService;
  private final UserQueryService userQueryService;
  private final GenerationQueryService generationQueryService;

  /** 11.1. */
  public PageResponse<AdminQuestionResult.ListRow> search(
      QnaStatus status, boolean siteOnly, Long lectureId, String keyword, Pageable pageable) {
    String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
    String keywordLike = trimmed == null ? null : "%" + trimmed + "%";
    List<Long> authorIds = trimmed == null ? List.of() : findAuthorIdsByName(trimmed);

    Page<Question> page = questionRepository.search(status, siteOnly, lectureId, keywordLike, authorIds, pageable);
    List<Question> questions = page.getContent();

    Map<Long, UserAccount> accounts = resolveAccounts(
        questions.stream().map(Question::getAuthorId).distinct().toList());
    Map<Long, String> lectureTitles = lectureQueryService.findTitlesByIds(
        questions.stream().map(Question::getLectureId).filter(Objects::nonNull).distinct().toList());

    int base = page.getNumber() * page.getSize();
    List<AdminQuestionResult.ListRow> rows = new ArrayList<>();
    for (int i = 0; i < questions.size(); i++) {
      Question question = questions.get(i);
      UserAccount account = accounts.get(question.getAuthorId());
      rows.add(new AdminQuestionResult.ListRow(
          base + i + 1,
          question.getId(),
          account != null ? account.name() : "UNKNOWN",
          account != null ? account.major() : null,
          question.getContent(),
          QnaDateTimes.toOffset(question.getCreatedAt()),
          question.getStatus(),
          question.getStatus().getLabel(),
          question.getLectureId() == null ? null : lectureTitles.get(question.getLectureId())));
    }
    return new PageResponse<>(rows, page.getNumber(), page.getSize(),
        page.getTotalElements(), page.getTotalPages(), page.isFirst(), page.isLast());
  }

  /** 11.2. */
  public AdminQuestionResult.Detail getDetail(Long questionId) {
    Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new BusinessException(QnaErrorCode.QUESTION_NOT_FOUND));
    UserAccount author = userAccountService.findActiveById(question.getAuthorId()).orElse(null);

    AdminQuestionResult.LectureBrief lecture = question.getLectureId() == null ? null
        : lectureQueryService.findTitlesByIds(List.of(question.getLectureId())).entrySet().stream()
            .findFirst()
            .map(entry -> new AdminQuestionResult.LectureBrief(entry.getKey(), entry.getValue()))
            .orElse(null);

    AdminQuestionResult.AnswerView answerView = answerRepository.findByQuestionId(questionId)
        .map(answer -> new AdminQuestionResult.AnswerView(
            answer.getId(), answer.getAdminId(), resolveName(answer.getAdminId()), answer.getContent(),
            QnaDateTimes.toOffset(answer.getCreatedAt()), QnaDateTimes.toOffset(answer.getUpdatedAt())))
        .orElse(null);

    return new AdminQuestionResult.Detail(
        question.getId(),
        new AdminQuestionResult.Author(
            question.getAuthorId(),
            author != null ? author.name() : "UNKNOWN",
            author != null ? author.college() : null,
            author != null ? author.major() : null,
            author != null ? author.role() : null),
        QnaDateTimes.toOffset(question.getCreatedAt()),
        question.getContent(),
        question.getStatus(),
        lecture,
        answerView);
  }

  /** 11.3. 첫 답변 시 question.status → ANSWERED. */
  @Transactional
  public AdminAnswerResult.CreateResult createAnswer(
      Long questionId, AdminAnswerRequest.Write request, Long adminId) {
    Question question = questionRepository.findById(questionId)
        .orElseThrow(() -> new BusinessException(QnaErrorCode.QUESTION_NOT_FOUND));
    if (answerRepository.existsByQuestionId(questionId)) {
      throw new BusinessException(QnaErrorCode.ALREADY_ANSWERED);
    }
    Answer answer = answerRepository.save(Answer.create(questionId, adminId, request.content()));
    question.markAnswered();
    return AdminAnswerResult.CreateResult.of(answer, resolveName(adminId));
  }

  /** 11.4. 작성자 본인만. */
  @Transactional
  public AdminAnswerResult.UpdateResult updateAnswer(
      Long questionId, AdminAnswerRequest.Write request, Long adminId) {
    Answer answer = answerRepository.findByQuestionId(questionId)
        .orElseThrow(() -> new BusinessException(QnaErrorCode.ANSWER_NOT_FOUND));
    if (!answer.isWrittenBy(adminId)) {
      throw new BusinessException(CommonErrorCode.NOT_RESOURCE_OWNER);
    }
    answer.update(request.content());
    answerRepository.flush();
    return AdminAnswerResult.UpdateResult.from(answer);
  }

  private List<Long> findAuthorIdsByName(String keyword) {
    return generationQueryService.findActive()
        .map(generation -> userQueryService.findActiveMembers(generation.generationNo()).stream()
            .filter(member -> member.userName() != null && member.userName().contains(keyword))
            .map(MemberSummary::userId)
            .toList())
        .orElseGet(List::of);
  }

  // userId 별 계정을 개별 조회한다. user 도메인에 findActiveByIds(#28 협의) 가 생기면 일괄 조회로 교체.
  private Map<Long, UserAccount> resolveAccounts(Collection<Long> userIds) {
    Map<Long, UserAccount> accounts = new HashMap<>();
    userIds.forEach(id -> userAccountService.findActiveById(id).ifPresent(account -> accounts.put(id, account)));
    return accounts;
  }

  private String resolveName(long userId) {
    return userAccountService.findActiveById(userId).map(UserAccount::name).orElse("UNKNOWN");
  }
}
