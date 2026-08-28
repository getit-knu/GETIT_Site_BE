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
}
