package com.getit.domain.lecture.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.admin.dto.FeedbackRequest;
import com.getit.domain.lecture.admin.dto.FeedbackResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.lecture.util.KstDateTimes;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FeedbackServiceTest {

  @Autowired
  private FeedbackService feedbackService;

  @Autowired
  private FeedbackRepository feedbackRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private UserRepository userRepository;

  private Long submissionId;
  private Long adminId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
    Long lectureId = lectureRepository.save(Lecture.create(
        1, "테스트 강의", null, null, null, null, true, generation.getId(), trackId, subCategoryId, 1L)).getId();
    Long assignmentId = assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", LocalDateTime.now().plusDays(7), Set.of(SubmissionType.LINK), null)).getId();
    submissionId = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignmentId, 100L, null, "https://github.com/user/repo", null,
        SubmissionStatus.SUBMITTED, LocalDateTime.now())).getId();

    User admin = User.createGuest("admin-1", "admin-1@getit.com", "관리자", null);
    admin.updateRole(Role.ADMIN);
    adminId = userRepository.save(admin).getId();
  }

  @Nested
  class Create {

    @Test
    @DisplayName("피드백을 작성한다")
    void createsFeedback() {
      FeedbackResult.CreateResult result = feedbackService.create(
          submissionId, new FeedbackRequest.Write("잘했습니다"), adminId);

      assertThat(result.submissionId()).isEqualTo(submissionId);
      assertThat(result.adminName()).isEqualTo("관리자");
      assertThat(result.content()).isEqualTo("잘했습니다");
    }

    @Test
    @DisplayName("존재하지 않는 제출물이면 예외가 발생한다")
    void throwsWhenSubmissionNotFound() {
      assertThatThrownBy(() -> feedbackService.create(
          999_999L, new FeedbackRequest.Write("잘했습니다"), adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBMISSION_NOT_FOUND);
    }

    @Test
    @DisplayName("작성자 계정이 비활성이면 adminName 은 UNKNOWN 이다")
    void adminNameFallsBackToUnknown() {
      User withdrawn = User.createGuest("gone", "gone@getit.com", "탈퇴자", null);
      withdrawn.updateRole(Role.ADMIN);
      withdrawn.withdraw();
      Long withdrawnId = userRepository.save(withdrawn).getId();

      FeedbackResult.CreateResult result = feedbackService.create(
          submissionId, new FeedbackRequest.Write("내용"), withdrawnId);

      assertThat(result.adminName()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("한 제출물에 여러 건 작성할 수 있다")
    void allowsMultipleFeedbacks() {
      feedbackService.create(submissionId, new FeedbackRequest.Write("첫 피드백"), adminId);
      feedbackService.create(submissionId, new FeedbackRequest.Write("두번째 피드백"), adminId);

      assertThat(feedbackRepository.findAllBySubmissionIdOrderByIdAsc(submissionId)).hasSize(2);
    }
  }

  @Nested
  class Update {

    @Test
    @DisplayName("작성자 본인이면 수정된다")
    void updatesWhenAuthor() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("원래 내용"), adminId);

      FeedbackResult.UpdateResult result = feedbackService.update(
          created.id(), new FeedbackRequest.Write("수정된 내용"), adminId);

      assertThat(result.content()).isEqualTo("수정된 내용");
    }

    @Test
    @DisplayName("응답의 updatedAt 은 이번 수정 시각을 반영한다")
    void updatedAtReflectsThisUpdate() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("원래 내용"), adminId);

      FeedbackResult.UpdateResult result = feedbackService.update(
          created.id(), new FeedbackRequest.Write("수정된 내용"), adminId);

      LocalDateTime persisted = feedbackRepository.findById(created.id()).orElseThrow().getUpdatedAt();
      assertThat(result.updatedAt()).isEqualTo(KstDateTimes.toOffset(persisted));
    }

    @Test
    @DisplayName("작성자가 아니면 예외가 발생한다")
    void throwsWhenNotAuthor() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("원래 내용"), adminId);

      assertThatThrownBy(() -> feedbackService.update(
          created.id(), new FeedbackRequest.Write("수정된 내용"), 999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
    }

    @Test
    @DisplayName("존재하지 않는 피드백이면 예외가 발생한다")
    void throwsWhenFeedbackNotFound() {
      assertThatThrownBy(() -> feedbackService.update(
          999_999L, new FeedbackRequest.Write("내용"), adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.FEEDBACK_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("delete")
  class Delete {

    @Test
    @DisplayName("작성자 본인이면 삭제된다")
    void deletesWhenAuthor() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("지울 내용"), adminId);

      feedbackService.delete(created.id(), adminId);

      assertThat(feedbackRepository.findById(created.id())).isEmpty();
    }

    @Test
    @DisplayName("한 건만 지우고 나머지는 남긴다")
    void deletesOnlyTheGivenFeedback() {
      FeedbackResult.CreateResult first = feedbackService.create(
          submissionId, new FeedbackRequest.Write("첫 번째"), adminId);
      FeedbackResult.CreateResult second = feedbackService.create(
          submissionId, new FeedbackRequest.Write("두 번째"), adminId);

      feedbackService.delete(first.id(), adminId);

      assertThat(feedbackRepository.findAllBySubmissionIdOrderByIdAsc(submissionId))
          .extracting(feedback -> feedback.getId())
          .containsExactly(second.id());
    }

    @Test
    @DisplayName("마지막 한 건을 지우면 제출물이 다시 피드백 없음으로 돌아간다")
    void submissionGoesBackToNoFeedback() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("내용"), adminId);
      assertThat(feedbackRepository.findSubmissionIdsWithFeedback(List.of(submissionId)))
          .contains(submissionId);

      feedbackService.delete(created.id(), adminId);

      // 제출 목록의 feedbackDone 이 이 조회로 계산된다. 지운 뒤에도 완료로 보이면 안 된다.
      assertThat(feedbackRepository.findSubmissionIdsWithFeedback(List.of(submissionId)))
          .doesNotContain(submissionId);
    }

    @Test
    @DisplayName("작성자가 아니면 예외가 발생한다")
    void throwsWhenNotAuthor() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("내용"), adminId);

      // 수정과 같은 규칙이다. 남의 피드백을 말없이 없앨 수 있으면
      // 부원은 피드백이 사라진 이유를 알 길이 없다.
      assertThatThrownBy(() -> feedbackService.delete(created.id(), 999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
      assertThat(feedbackRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 피드백이면 예외가 발생한다")
    void throwsWhenFeedbackNotFound() {
      assertThatThrownBy(() -> feedbackService.delete(999_999L, adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.FEEDBACK_NOT_FOUND);
    }

  }

  /**
   * 지난 기수의 자료는 읽기 전용이다. (이슈 #168 — PR #167 리뷰에서 시작된 결정)
   *
   * <p>어드민은 지난 기수를 계속 열람하므로 없는 것처럼 404 로 감추지 않고, 볼 수는 있지만
   * 고칠 수 없다는 뜻으로 409 를 준다. 부원용 {@code SubmissionService} 가 404 를 주는 것과
   * 다른 이유다 — 부원에게는 애초에 보이지 않는 자료다.
   *
   * <p>작성 · 수정 · 삭제에 모두 건다. 하나만 막으면 같은 피드백을 고칠 수는 있는데
   * 지울 수는 없는 상태가 된다.
   */
  @Nested
  @DisplayName("지난 기수는 읽기 전용")
  class PastGenerationIsReadOnly {

    /** 10기를 열어, setUp 의 강의가 속한 9기를 지난 기수로 만든다. */
    private void startNewGeneration() {
      generationRepository.findAll().stream()
          .filter(Generation::isActive)
          .forEach(Generation::deactivate);

      Generation next = Generation.create(10, 2027);
      next.activate();
      generationRepository.saveAndFlush(next);
    }

    @Test
    @DisplayName("지난 기수 제출물에는 피드백을 쓸 수 없다")
    void cannotCreate() {
      startNewGeneration();

      assertThatThrownBy(() -> feedbackService.create(
          submissionId, new FeedbackRequest.Write("뒤늦은 피드백"), adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.PAST_GENERATION_READ_ONLY);
    }

    @Test
    @DisplayName("지난 기수 피드백은 수정할 수 없다")
    void cannotUpdate() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("원래 내용"), adminId);
      startNewGeneration();

      assertThatThrownBy(() -> feedbackService.update(
          created.id(), new FeedbackRequest.Write("고친 내용"), adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.PAST_GENERATION_READ_ONLY);
    }

    @Test
    @DisplayName("지난 기수 피드백은 삭제할 수 없고 내용이 그대로 남는다")
    void cannotDeleteAndDataSurvives() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("지난 기수 피드백"), adminId);
      startNewGeneration();

      assertThatThrownBy(() -> feedbackService.delete(created.id(), adminId))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.PAST_GENERATION_READ_ONLY);

      assertThat(feedbackRepository.findById(created.id()).orElseThrow().getContent())
          .isEqualTo("지난 기수 피드백");
    }

    @Test
    @DisplayName("남의 피드백이면 기수를 따지기 전에 먼저 막힌다")
    void ownershipIsCheckedFirst() {
      FeedbackResult.CreateResult created = feedbackService.create(
          submissionId, new FeedbackRequest.Write("내용"), adminId);
      startNewGeneration();

      assertThatThrownBy(() -> feedbackService.delete(created.id(), 999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.NOT_RESOURCE_OWNER);
    }
  }
}
