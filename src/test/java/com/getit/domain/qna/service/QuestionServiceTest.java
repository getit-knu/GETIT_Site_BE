package com.getit.domain.qna.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.qna.dto.MemberQuestionRequest;
import com.getit.domain.qna.dto.MemberQuestionResult;
import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.repository.AnswerRepository;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import com.getit.global.dto.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuestionServiceTest {

  private static final int ACTIVE_GENERATION_NO = 9;

  @Autowired
  private QuestionService questionService;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private UserRepository userRepository;

  private Long activeGenerationId;
  private Long memberId;
  private Long otherMemberId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(ACTIVE_GENERATION_NO, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();
    memberId = member("member").getId();
    otherMemberId = member("other").getId();
  }

  private User member(String providerId) {
    User user = User.createGuest(providerId, providerId + "@getit.com", providerId + " 님", null);
    user.promoteToMember(ACTIVE_GENERATION_NO);
    return userRepository.save(user);
  }

  private Lecture lecture(boolean published) {
    return lectureRepository.save(Lecture.create(
        1, "1주차", null, null, null, null, published, activeGenerationId, null, null, 1L));
  }

  @Test
  @DisplayName("질문을 등록하면 PENDING 으로 저장된다")
  void createsPendingQuestion() {
    Long lectureId = lecture(true).getId();

    MemberQuestionResult.CreateResult result =
        questionService.create(lectureId, new MemberQuestionRequest.Create("질문이요"), memberId);

    assertThat(result.status()).isEqualTo(QnaStatus.PENDING);
    assertThat(questionRepository.findById(result.id()).orElseThrow().getAuthorId()).isEqualTo(memberId);
  }

  @Test
  @DisplayName("비공개 강의면 404 다 (접근 가드)")
  void rejectsUnpublishedLecture() {
    Long lectureId = lecture(false).getId();

    assertThatThrownBy(() ->
        questionService.create(lectureId, new MemberQuestionRequest.Create("질문"), memberId))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode")
        .isEqualTo(LectureErrorCode.LECTURE_NOT_FOUND);
  }

  @Test
  @DisplayName("목록은 본인 질문만, 답변을 붙여서 반환한다")
  void listsOwnQuestionsWithAnswers() {
    Long lectureId = lecture(true).getId();
    Question mine = questionRepository.save(Question.create(memberId, lectureId, "내 질문"));
    questionRepository.save(Question.create(otherMemberId, lectureId, "남의 질문"));
    answerRepository.save(Answer.create(mine.getId(), 100L, "답변드립니다"));

    List<MemberQuestionResult.ListItem> result = questionService.getMyQuestions(lectureId, memberId);

    assertThat(result).extracting(MemberQuestionResult.ListItem::content).containsExactly("내 질문");
    assertThat(result.get(0).answers()).extracting(MemberQuestionResult.AnswerItem::content)
        .containsExactly("답변드립니다");
  }

  @Test
  @DisplayName("질문이 없으면 빈 리스트다")
  void emptyWhenNoQuestions() {
    Long lectureId = lecture(true).getId();

    assertThat(questionService.getMyQuestions(lectureId, memberId)).isEmpty();
  }

  /**
   * 내 질문 전체 조회. 강의를 가로지른다. (이슈 #185)
   *
   * <p>강의별 조회(4.6)와 같은 규칙 — 내 질문만 — 이지만, 강의에 매이지 않고 페이징 · 정렬을
   * 서버가 정하며 응답에 강의 정보가 실린다.
   */
  @Nested
  @DisplayName("getMyQuestions (전체)")
  class MyQuestionsAcrossLectures {

    @Test
    @DisplayName("여러 강의의 내 질문을 최신순으로 모아 준다")
    void collectsAcrossLectures() {
      Lecture first = lecture(true);
      Lecture second = lectureRepository.save(Lecture.create(
          2, "2주차", null, null, null, null, true, activeGenerationId, null, null, 1L));
      questionRepository.save(Question.create(memberId, first.getId(), "먼저 쓴 질문"));
      Question latest = questionRepository.save(
          Question.create(memberId, second.getId(), "나중에 쓴 질문"));

      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(MemberQuestionResult.MyListItem::content)
          .containsExactly("나중에 쓴 질문", "먼저 쓴 질문");
      assertThat(result.content().get(0).id()).isEqualTo(latest.getId());
    }

    @Test
    @DisplayName("어느 강의의 질문인지 함께 준다")
    void carriesLectureInfo() {
      Lecture lecture = lecture(true);
      questionRepository.save(Question.create(memberId, lecture.getId(), "질문"));

      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, null, PageRequest.of(0, 20));

      // 강의를 가로지르는 목록이라 이게 없으면 어느 강의 질문인지 구분이 안 된다.
      assertThat(result.content().get(0).lectureId()).isEqualTo(lecture.getId());
      assertThat(result.content().get(0).lectureTitle()).isEqualTo("1주차");
    }

    @Test
    @DisplayName("남의 질문은 섞이지 않는다")
    void excludesOtherPeoplesQuestions() {
      Long lectureId = lecture(true).getId();
      questionRepository.save(Question.create(memberId, lectureId, "내 질문"));
      questionRepository.save(Question.create(otherMemberId, lectureId, "남의 질문"));

      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(MemberQuestionResult.MyListItem::content)
          .containsExactly("내 질문");
    }

    @Test
    @DisplayName("status 로 답변 대기만 거를 수 있다")
    void filtersByStatus() {
      Long lectureId = lecture(true).getId();
      Question pending = questionRepository.save(Question.create(memberId, lectureId, "대기 중"));
      Question answered = questionRepository.save(Question.create(memberId, lectureId, "답변 됨"));
      answered.markAnswered();
      questionRepository.flush();

      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, QnaStatus.PENDING, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(MemberQuestionResult.MyListItem::id)
          .containsExactly(pending.getId());
    }

    @Test
    @DisplayName("비공개로 바뀐 강의의 질문도 내 목록에는 남는다")
    void keepsQuestionsFromUnpublishedLectures() {
      Lecture hidden = lecture(false);
      questionRepository.save(Question.create(memberId, hidden.getId(), "쓸 때는 공개였다"));

      // 강의 접근 권한을 다시 보지 않는다. 내가 남긴 글이 나중에 사라지면 안 된다.
      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, null, PageRequest.of(0, 20));

      assertThat(result.content()).hasSize(1);
    }

    @Test
    @DisplayName("질문이 없으면 빈 목록이다")
    void returnsEmptyWhenNoQuestions() {
      PageResponse<MemberQuestionResult.MyListItem> result =
          questionService.getMyQuestions(memberId, null, PageRequest.of(0, 20));

      assertThat(result.content()).isEmpty();
    }
  }
}
