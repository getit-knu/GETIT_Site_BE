package com.getit.domain.lecture.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.lecture.admin.dto.SubmissionDetailResult;
import com.getit.domain.lecture.admin.dto.SubmissionOverviewResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Set;
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
class SubmissionAdminServiceTest {

  @Autowired
  private SubmissionAdminService submissionAdminService;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private FeedbackRepository feedbackRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  private Integer activeGenerationNo;
  private Long lectureId;
  private Long assignmentId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationRepository.save(generation);
    activeGenerationNo = 9;

    Long trackId = trackRepository.save(Track.create("SW", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("웹기초", 1, trackId)).getId();
    lectureId = lectureRepository.save(Lecture.create(
        1, "테스트 강의", null, null, null, null, true, generation.getId(), trackId, subCategoryId, 1L)).getId();
    assignmentId = assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", LocalDateTime.now().plusDays(7), Set.of(SubmissionType.LINK), null)).getId();
  }

  private User createMember(String providerId, String name, String major) {
    User user = User.createGuest(providerId, providerId + "@getit.com", name, null);
    user.updateApplicantInfo(null, null, major, null, null);
    user.promoteToMember(activeGenerationNo);
    return userRepository.save(user);
  }

  private User createAdmin(String providerId) {
    User admin = User.createGuest(providerId, providerId + "@getit.com", "관리자", null);
    admin.updateRole(com.getit.domain.user.entity.Role.ADMIN);
    return userRepository.save(admin);
  }

  @Nested
  class GetOverview {

    @Test
    @DisplayName("제출자와 미제출자를 함께 반환하고 통계를 계산한다")
    void returnsSubmittedAndNotSubmitted() {
      User submitted = createMember("sub-1", "제출자", "컴퓨터공학과");
      createMember("sub-2", "미제출자", "경영학과");
      assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, submitted.getId(), null, "https://github.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));

      SubmissionOverviewResult.Overview overview = submissionAdminService.getOverview(
          lectureId, null, null, null, PageRequest.of(0, 50));

      assertThat(overview.counts().total()).isEqualTo(2);
      assertThat(overview.counts().submitted()).isEqualTo(1);
      assertThat(overview.counts().notSubmitted()).isEqualTo(1);
      assertThat(overview.content()).hasSize(2);
    }

    @Test
    @DisplayName("submitted 필터를 적용하면 제출자만 반환한다")
    void filtersBySubmitted() {
      User submitted = createMember("sub-3", "제출자", "컴퓨터공학과");
      createMember("sub-4", "미제출자", "경영학과");
      assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, submitted.getId(), null, "https://github.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));

      SubmissionOverviewResult.Overview overview = submissionAdminService.getOverview(
          lectureId, true, null, null, PageRequest.of(0, 50));

      assertThat(overview.content()).hasSize(1);
      assertThat(overview.content().get(0).userName()).isEqualTo("제출자");
    }

    @Test
    @DisplayName("과제가 없는 강의면 예외가 발생한다")
    void throwsWhenAssignmentNotFound() {
      Long lectureWithoutAssignment = lectureRepository.save(Lecture.create(
          2, "과제없는강의", null, null, null, null, true,
          lectureRepository.findById(lectureId).orElseThrow().getGenerationId(),
          lectureRepository.findById(lectureId).orElseThrow().getTrackId(),
          lectureRepository.findById(lectureId).orElseThrow().getSubCategoryId(), 1L)).getId();

      assertThatThrownBy(() -> submissionAdminService.getOverview(
          lectureWithoutAssignment, null, null, null, PageRequest.of(0, 50)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.ASSIGNMENT_NOT_FOUND);
    }
  }

  @Nested
  class GetDetail {

    @Test
    @DisplayName("제출물 상세와 피드백·탐색 정보를 반환한다")
    void returnsDetailWithFeedbackAndNavigation() {
      User member = createMember("sub-5", "제출자", "컴퓨터공학과");
      User admin = createAdmin("admin-1");
      AssignmentSubmission submission = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, member.getId(), null, "https://github.com/user/repo", "코멘트",
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));
      feedbackRepository.save(Feedback.create(submission.getId(), admin.getId(), "잘했습니다"));

      SubmissionDetailResult.Detail detail = submissionAdminService.getDetail(submission.getId());

      assertThat(detail.user().name()).isEqualTo("제출자");
      assertThat(detail.linkUrl()).isEqualTo("https://github.com/user/repo");
      assertThat(detail.feedbacks()).hasSize(1);
      assertThat(detail.feedbacks().get(0).adminName()).isEqualTo("관리자");
      assertThat(detail.navigation().total()).isEqualTo(1);
    }

    @Test
    @DisplayName("피드백 작성 관리자를 찾을 수 없으면 이름을 UNKNOWN 으로 반환한다")
    void returnsUnknownWhenAdminNotFound() {
      User member = createMember("sub-7", "제출자", "컴퓨터공학과");
      AssignmentSubmission submission = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, member.getId(), null, "https://github.com/user/repo", "코멘트",
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));
      feedbackRepository.save(Feedback.create(submission.getId(), 999_999L, "탈퇴한 관리자의 피드백"));

      SubmissionDetailResult.Detail detail = submissionAdminService.getDetail(submission.getId());

      assertThat(detail.feedbacks()).hasSize(1);
      assertThat(detail.feedbacks().get(0).adminName()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("이미지·PDF 파일이면 previewable 이 true 다")
    void marksPreviewableForImageFile() {
      User member = createMember("sub-6", "제출자", "컴퓨터공학과");
      Long fileId = fileAssetRepository.save(
          FileAsset.upload("key/1", "과제.png", "https://cdn/key/1", 1024L, "image/png", member.getId())).getId();
      AssignmentSubmission submission = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, member.getId(), fileId, null, null, SubmissionStatus.SUBMITTED, LocalDateTime.now()));

      SubmissionDetailResult.Detail detail = submissionAdminService.getDetail(submission.getId());

      assertThat(detail.file().previewable()).isTrue();
      assertThat(detail.file().previewUrl()).isNotNull();
    }

    @Test
    @DisplayName("존재하지 않는 제출물이면 예외가 발생한다")
    void throwsWhenSubmissionNotFound() {
      assertThatThrownBy(() -> submissionAdminService.getDetail(999_999L))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.SUBMISSION_NOT_FOUND);
    }
  }

  @Nested
  class Navigate {

    @Test
    @DisplayName("현재 제출물 기준 이전·다음 제출물을 반환한다")
    void returnsPrevAndNext() {
      User memberA = createMember("nav-1", "가나다", "컴퓨터공학과");
      User memberB = createMember("nav-2", "라마바", "경영학과");
      AssignmentSubmission submissionA = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, memberA.getId(), null, "https://github.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));
      AssignmentSubmission submissionB = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignmentId, memberB.getId(), null, "https://gitlab.com/user/repo", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));
      Long firstId = memberA.getId() < memberB.getId() ? submissionA.getId() : submissionB.getId();
      Long secondId = memberA.getId() < memberB.getId() ? submissionB.getId() : submissionA.getId();

      SubmissionDetailResult.Navigation navigation =
          submissionAdminService.navigate(lectureId, firstId, null, null, null);

      assertThat(navigation.total()).isEqualTo(2);
      assertThat(navigation.prevSubmissionId()).isNull();
      assertThat(navigation.nextSubmissionId()).isEqualTo(secondId);
    }
  }
}
