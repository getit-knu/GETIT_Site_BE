package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.lecture.dto.LectureResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.LectureFile;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureFileRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
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
class LectureServiceTest {

  @Autowired
  private LectureService lectureService;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private LectureFileRepository lectureFileRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private FeedbackRepository feedbackRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private FileAssetRepository fileAssetRepository;

  @Autowired
  private UserRepository userRepository;

  private static final int ACTIVE_GENERATION_NO = 9;

  private Long activeGenerationId;
  private Long trackId;
  private Long subCategoryId;
  private Long memberId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(ACTIVE_GENERATION_NO, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();

    trackId = trackRepository.save(Track.create("SW", 1)).getId();
    subCategoryId = subCategoryRepository.save(SubCategory.create("WEB 기초", 1, trackId)).getId();
    memberId = member("member", ACTIVE_GENERATION_NO).getId();
  }

  private User member(String providerId, int generationNo) {
    User user = User.createGuest(providerId, providerId + "@getit.com", providerId + " 님", null);
    user.promoteToMember(generationNo);
    return userRepository.save(user);
  }

  private Lecture lecture(boolean published, Long generationId, Long createdBy) {
    return lectureRepository.save(Lecture.create(
        1, "1주차 강의", "## 본문", "https://youtu.be/x", "https://docs/x", 120,
        published, generationId, trackId, subCategoryId, createdBy));
  }

  private Assignment assignment(Long lectureId, LocalDateTime deadline) {
    return assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", deadline, java.util.Set.of(
            com.getit.domain.lecture.entity.SubmissionType.LINK), null));
  }

  @Nested
  class GetLectures {

    @Test
    @DisplayName("공개된 활성 기수 강의만 목록에 나온다")
    void listsOnlyPublishedLecturesOfActiveGeneration() {
      lecture(true, activeGenerationId, memberId);
      lecture(false, activeGenerationId, memberId);
      Generation past = generationRepository.save(Generation.create(8, 2025));
      lecture(true, past.getId(), memberId);

      LectureResult.ListResult result =
          lectureService.getLectures(memberId, null, null, PageRequest.of(0, 12));

      assertThat(result.content()).hasSize(1);
      assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("과제 제출 레코드가 있으면 completed=true, 없으면 false, 과제 자체가 없으면 false")
    void completedReflectsSubmissionPresence() {
      Lecture submitted = lecture(true, activeGenerationId, memberId);
      Lecture notSubmitted = lecture(true, activeGenerationId, memberId);
      Lecture noAssignment = lecture(true, activeGenerationId, memberId);
      Assignment doneAssignment = assignment(submitted.getId(), LocalDateTime.now().plusDays(3));
      assignment(notSubmitted.getId(), LocalDateTime.now().plusDays(3));
      assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          doneAssignment.getId(), memberId, null, "https://github.com/a/b", null,
          SubmissionStatus.SUBMITTED, LocalDateTime.now()));

      LectureResult.ListResult result =
          lectureService.getLectures(memberId, null, null, PageRequest.of(0, 12));

      assertThat(result.content())
          .filteredOn(content -> content.id().equals(submitted.getId()))
          .allMatch(LectureResult.Content::completed);
      assertThat(result.content())
          .filteredOn(content -> !content.id().equals(submitted.getId()))
          .noneMatch(LectureResult.Content::completed);
    }

    @Test
    @DisplayName("공개 강의가 있는 소분류만 탭으로 노출한다")
    void tabsListSubCategoriesWithPublishedLectures() {
      lecture(true, activeGenerationId, memberId);
      Long emptySubCategoryId = subCategoryRepository.save(SubCategory.create("빈 분류", 2, trackId)).getId();

      LectureResult.ListResult result =
          lectureService.getLectures(memberId, null, null, PageRequest.of(0, 12));

      assertThat(result.tabs()).extracting(LectureResult.Tab::subCategoryId).containsExactly(subCategoryId);
      assertThat(result.tabs()).extracting(LectureResult.Tab::subCategoryId).doesNotContain(emptySubCategoryId);
    }

    @Test
    @DisplayName("요청자의 기수가 활성 기수와 다르면 FORBIDDEN")
    void throwsForbiddenWhenRequesterNotInActiveGeneration() {
      Long outsiderId = member("outsider", 8).getId();

      assertThatThrownBy(() -> lectureService.getLectures(outsiderId, null, null, PageRequest.of(0, 12)))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.FORBIDDEN);
    }
  }

  @Nested
  class GetLecture {

    @Test
    @DisplayName("상세에 작성자·자료·과제·내 제출물과 피드백이 담긴다")
    void returnsDetailWithSubmissionAndFeedback() {
      User author = userRepository.save(User.createGuest("author", "author@getit.com", "작성자", "img"));
      User admin = userRepository.save(User.createGuest("admin", "admin@getit.com", "관리자", null));
      Lecture lecture = lecture(true, activeGenerationId, author.getId());
      Assignment assignment = assignment(lecture.getId(), LocalDateTime.now().plusDays(3));
      AssignmentSubmission submission = assignmentSubmissionRepository.save(AssignmentSubmission.submit(
          assignment.getId(), memberId, null, "https://github.com/a/b", "확인 부탁", SubmissionStatus.SUBMITTED,
          LocalDateTime.now()));
      feedbackRepository.save(Feedback.create(submission.getId(), admin.getId(), "좋습니다"));

      LectureResult.DetailResult result = lectureService.getLecture(memberId, lecture.getId());

      assertThat(result.author().name()).isEqualTo("작성자");
      assertThat(result.assignment().id()).isEqualTo(assignment.getId());
      assertThat(result.mySubmission().linkUrl()).isEqualTo("https://github.com/a/b");
      assertThat(result.mySubmission().feedbacks()).singleElement()
          .satisfies(feedback -> {
            assertThat(feedback.adminName()).isEqualTo("관리자");
            assertThat(feedback.content()).isEqualTo("좋습니다");
          });
    }

    @Test
    @DisplayName("작성자 계정이 탈퇴 상태면 author.name 은 UNKNOWN")
    void authorNameFallsBackToUnknown() {
      User author = userRepository.save(User.createGuest("author", "author@getit.com", "작성자", null));
      author.withdraw();
      Lecture lecture = lecture(true, activeGenerationId, author.getId());

      LectureResult.DetailResult result = lectureService.getLecture(memberId, lecture.getId());

      assertThat(result.author().name()).isEqualTo("UNKNOWN");
    }

    @Test
    @DisplayName("미제출이면 mySubmission 은 null")
    void mySubmissionNullWhenNotSubmitted() {
      Lecture lecture = lecture(true, activeGenerationId, memberId);
      assignment(lecture.getId(), LocalDateTime.now().plusDays(3));

      LectureResult.DetailResult result = lectureService.getLecture(memberId, lecture.getId());

      assertThat(result.mySubmission()).isNull();
    }

    @Test
    @DisplayName("비공개 강의는 404")
    void notFoundWhenUnpublished() {
      Lecture lecture = lecture(false, activeGenerationId, memberId);

      assertThatThrownBy(() -> lectureService.getLecture(memberId, lecture.getId()))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.LECTURE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 기수 강의는 404")
    void notFoundWhenOtherGeneration() {
      Generation past = generationRepository.save(Generation.create(8, 2025));
      Lecture lecture = lecture(true, past.getId(), memberId);

      assertThatThrownBy(() -> lectureService.getLecture(memberId, lecture.getId()))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", LectureErrorCode.LECTURE_NOT_FOUND);
    }
  }

  @Nested
  class GetMaterialDownloadUrl {

    @Test
    @DisplayName("연결된 자료면 평문 URL 과 expiresIn 300 을 반환한다")
    void returnsPlainUrl() {
      Lecture lecture = lecture(true, activeGenerationId, memberId);
      FileAsset file = fileAssetRepository.save(FileAsset.upload(
          "key/1", "자료.pdf", "https://cdn/key/1", 2048L, "application/pdf", memberId));
      lectureFileRepository.save(LectureFile.create("강의 자료.pdf", lecture.getId(), file.getId()));

      LectureResult.DownloadUrl result =
          lectureService.getMaterialDownloadUrl(memberId, lecture.getId(), file.getId());

      assertThat(result.downloadUrl()).isEqualTo("https://cdn/key/1");
      assertThat(result.fileName()).isEqualTo("강의 자료.pdf");
      assertThat(result.expiresIn()).isEqualTo(300);
    }

    @Test
    @DisplayName("강의에 연결되지 않은 fileId 면 404")
    void notFoundWhenFileNotLinked() {
      Lecture lecture = lecture(true, activeGenerationId, memberId);
      FileAsset file = fileAssetRepository.save(FileAsset.upload(
          "key/2", "무관.pdf", "https://cdn/key/2", 1024L, "application/pdf", memberId));

      assertThatThrownBy(() ->
          lectureService.getMaterialDownloadUrl(memberId, lecture.getId(), file.getId()))
          .isInstanceOf(BusinessException.class)
          .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    }
  }
}
