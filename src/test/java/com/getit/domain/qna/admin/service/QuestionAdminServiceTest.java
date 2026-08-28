package com.getit.domain.qna.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.qna.admin.dto.AdminAnswerRequest;
import com.getit.domain.qna.admin.dto.AdminAnswerResult;
import com.getit.domain.qna.admin.dto.AdminQuestionResult;
import com.getit.domain.qna.entity.Answer;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.entity.Question;
import com.getit.domain.qna.exception.QnaErrorCode;
import com.getit.domain.qna.repository.AnswerRepository;
import com.getit.domain.qna.repository.QuestionRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class QuestionAdminServiceTest {

  private static final int ACTIVE_GENERATION_NO = 9;

  @Autowired
  private QuestionAdminService questionAdminService;

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
  private Long alice;
  private Long bob;
  private Long adminId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(ACTIVE_GENERATION_NO, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();
    alice = member("alice", "김앨리스").getId();
    bob = member("bob", "이밥").getId();
    User admin = User.createGuest("admin", "admin@getit.com", "관리자", null);
    admin.updateRole(Role.ADMIN);
    adminId = userRepository.save(admin).getId();
  }

  private User member(String providerId, String name) {
    User user = User.createGuest(providerId, providerId + "@getit.com", name, null);
    user.promoteToMember(ACTIVE_GENERATION_NO);
    user.updateApplicantInfo("010", "공과대학", providerId + "학과", 3, "21");
    return userRepository.save(user);
  }

  private Long lectureId() {
    return lectureRepository.save(Lecture.create(
        1, "1주차", null, null, null, null, true, activeGenerationId, null, null, 1L)).getId();
  }

  private Question question(Long authorId, Long lectureId, String content) {
    return questionRepository.save(Question.create(authorId, lectureId, content));
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("status 로 거른다")
    void filtersByStatus() {
      question(alice, lectureId(), "대기중");
      Question answered = question(bob, lectureId(), "답변됨");
      answered.markAnswered();
      questionRepository.flush();

      var result = questionAdminService.search(
          QnaStatus.PENDING, false, null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(AdminQuestionResult.ListRow::content).containsExactly("대기중");
    }

    @Test
    @DisplayName("siteOnly 면 lectureId 가 null 인 질문만")
    void filtersSiteOnly() {
      question(alice, lectureId(), "강의 질문");
      question(alice, null, "사이트 질문");

      var result = questionAdminService.search(null, true, null, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(AdminQuestionResult.ListRow::content).containsExactly("사이트 질문");
    }

    @Test
    @DisplayName("lectureId 로 거른다")
    void filtersByLectureId() {
      Long target = lectureId();
      question(alice, target, "타깃 강의");
      question(alice, lectureId(), "다른 강의");

      var result = questionAdminService.search(null, false, target, null, PageRequest.of(0, 20));

      assertThat(result.content()).extracting(AdminQuestionResult.ListRow::content).containsExactly("타깃 강의");
    }

    @Test
    @DisplayName("keyword 는 내용 또는 작성자 이름에 매칭된다")
    void filtersByKeywordContentOrAuthorName() {
      question(alice, lectureId(), "스프링 부트 질문");
      question(bob, lectureId(), "전혀 다른 내용");

      var byContent = questionAdminService.search(null, false, null, "스프링", PageRequest.of(0, 20));
      var byName = questionAdminService.search(null, false, null, "이밥", PageRequest.of(0, 20));

      assertThat(byContent.content()).extracting(AdminQuestionResult.ListRow::content)
          .containsExactly("스프링 부트 질문");
      assertThat(byName.content()).extracting(AdminQuestionResult.ListRow::content)
          .containsExactly("전혀 다른 내용");
    }

    @Test
    @DisplayName("no 는 페이지 기준 연속번호다")
    void computesPageRelativeNo() {
      for (int i = 0; i < 3; i++) {
        question(alice, lectureId(), "q" + i);
      }

      var page1 = questionAdminService.search(null, false, null, null, PageRequest.of(1, 2));

      assertThat(page1.content()).extracting(AdminQuestionResult.ListRow::no).containsExactly(3);
    }
  }

  @Nested
  @DisplayName("getDetail")
  class GetDetail {

    @Test
    @DisplayName("작성자 · 강의 · 답변을 채운다")
    void populatesAuthorLectureAnswer() {
      Long lectureId = lectureId();
      Question question = question(alice, lectureId, "질문");
      answerRepository.save(Answer.create(question.getId(), adminId, "답변"));

      var detail = questionAdminService.getDetail(question.getId());

      assertThat(detail.author().name()).isEqualTo("김앨리스");
      assertThat(detail.lecture().id()).isEqualTo(lectureId);
      assertThat(detail.answer().content()).isEqualTo("답변");
      assertThat(detail.answer().adminName()).isEqualTo("관리자");
    }

    @Test
    @DisplayName("사이트 질문이면 lecture 는 null 이다")
    void nullLectureForSiteQuestion() {
      Question question = question(alice, null, "사이트 질문");

      assertThat(questionAdminService.getDetail(question.getId()).lecture()).isNull();
    }

    @Test
    @DisplayName("없는 질문이면 예외")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> questionAdminService.getDetail(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(QnaErrorCode.QUESTION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("createAnswer")
  class CreateAnswer {

    @Test
    @DisplayName("답변을 달면 질문이 ANSWERED 가 된다")
    void marksQuestionAnswered() {
      Question question = question(alice, lectureId(), "질문");

      AdminAnswerResult.CreateResult result = questionAdminService.createAnswer(
          question.getId(), new AdminAnswerRequest.Write("답변드립니다"), adminId);

      assertThat(result.questionStatus()).isEqualTo(QnaStatus.ANSWERED);
      assertThat(questionRepository.findById(question.getId()).orElseThrow().getStatus())
          .isEqualTo(QnaStatus.ANSWERED);
    }

    @Test
    @DisplayName("이미 답변이 있으면 409")
    void rejectsSecondAnswer() {
      Question question = question(alice, lectureId(), "질문");
      answerRepository.save(Answer.create(question.getId(), adminId, "첫 답변"));

      assertThatThrownBy(() -> questionAdminService.createAnswer(
          question.getId(), new AdminAnswerRequest.Write("두번째"), adminId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(QnaErrorCode.ALREADY_ANSWERED);
    }

    @Test
    @DisplayName("없는 질문이면 예외")
    void throwsWhenQuestionNotFound() {
      assertThatThrownBy(() -> questionAdminService.createAnswer(
          999L, new AdminAnswerRequest.Write("답변"), adminId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(QnaErrorCode.QUESTION_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("updateAnswer")
  class UpdateAnswer {

    @Test
    @DisplayName("작성자 본인은 수정할 수 있다")
    void ownerUpdates() {
      Question question = question(alice, lectureId(), "질문");
      answerRepository.save(Answer.create(question.getId(), adminId, "원래 답변"));

      AdminAnswerResult.UpdateResult result = questionAdminService.updateAnswer(
          question.getId(), new AdminAnswerRequest.Write("수정된 답변"), adminId);

      assertThat(result.content()).isEqualTo("수정된 답변");
    }

    @Test
    @DisplayName("다른 관리자면 403")
    void rejectsNonOwner() {
      Question question = question(alice, lectureId(), "질문");
      answerRepository.save(Answer.create(question.getId(), adminId, "답변"));

      assertThatThrownBy(() -> questionAdminService.updateAnswer(
          question.getId(), new AdminAnswerRequest.Write("수정"), 999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    @Test
    @DisplayName("답변이 없으면 예외")
    void throwsWhenNoAnswer() {
      Question question = question(alice, lectureId(), "질문");

      assertThatThrownBy(() -> questionAdminService.updateAnswer(
          question.getId(), new AdminAnswerRequest.Write("수정"), adminId))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(QnaErrorCode.ANSWER_NOT_FOUND);
    }
  }
}
