package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.lecture.dto.MeSummaryResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
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
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MeSummaryServiceTest {

  @Autowired
  private MeSummaryService meSummaryService;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

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
    User user = User.createGuest(providerId, providerId + "@getit.com", providerId + " 님", "https://img");
    user.promoteToMember(generationNo);
    user.updateApplicantInfo("010", "경영대학", "경영학과", 3, "21");
    return userRepository.save(user);
  }

  private Lecture lecture(int week, boolean published, Long generationId) {
    return lectureRepository.save(Lecture.create(
        week, week + "주차", null, null, null, null, published, generationId, trackId, subCategoryId, 1L));
  }

  private Assignment assignment(Long lectureId, LocalDateTime deadline) {
    return assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", deadline, Set.of(SubmissionType.LINK), null));
  }

  private void submit(Long assignmentId, Long userId, SubmissionStatus status) {
    assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignmentId, userId, null, "https://github.com/a/b", null, status, LocalDateTime.now()));
  }

  @Test
  @DisplayName("enrolledLectureCount 는 활성 기수의 공개 강의 수만 센다")
  void enrolledLectureCountCountsPublishedOnly() {
    lecture(1, true, activeGenerationId);
    lecture(2, true, activeGenerationId);
    lecture(3, false, activeGenerationId);
    Generation past = generationRepository.save(Generation.create(8, 2025));
    lecture(4, true, past.getId());

    MeSummaryResult.Response result = meSummaryService.getSummary(memberId);

    assertThat(result.stats().enrolledLectureCount()).isEqualTo(2);
  }

  @Test
  @DisplayName("submittedAssignmentCount 는 본인 제출만, LATE 포함해서 센다")
  void submittedAssignmentCountIncludesLateAndExcludesOthers() {
    Long a1 = assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
    Long a2 = assignment(lecture(2, true, activeGenerationId).getId(), LocalDateTime.now().minusDays(1)).getId();
    submit(a1, memberId, SubmissionStatus.SUBMITTED);
    submit(a2, memberId, SubmissionStatus.LATE);
    submit(a1, 999L, SubmissionStatus.SUBMITTED);

    MeSummaryResult.Response result = meSummaryService.getSummary(memberId);

    assertThat(result.stats().submittedAssignmentCount()).isEqualTo(2);
    assertThat(result.stats().lateSubmittedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("마감 지났고 미제출인 과제만 notSubmitted 에 들어간다")
  void notSubmittedIsPastDeadlineAndNoRecord() {
    Lecture overdue = lecture(1, true, activeGenerationId);
    Lecture future = lecture(2, true, activeGenerationId);
    Lecture done = lecture(3, true, activeGenerationId);
    assignment(overdue.getId(), LocalDateTime.now().minusDays(1));
    assignment(future.getId(), LocalDateTime.now().plusDays(1));
    Long doneAssignmentId = assignment(done.getId(), LocalDateTime.now().minusDays(1)).getId();
    submit(doneAssignmentId, memberId, SubmissionStatus.LATE);

    MeSummaryResult.Response result = meSummaryService.getSummary(memberId);

    assertThat(result.notSubmittedLectures()).extracting(MeSummaryResult.LectureBrief::lectureId)
        .containsExactly(overdue.getId());
    assertThat(result.stats().notSubmittedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("lateSubmittedLectures 는 본인 제출이 LATE 인 강의만, week 순으로 담긴다")
  void lateSubmittedLecturesOrderedByWeek() {
    Long a5 = assignment(lecture(5, true, activeGenerationId).getId(), LocalDateTime.now().minusDays(1)).getId();
    Long a1 = assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().minusDays(1)).getId();
    Long a2 = assignment(lecture(2, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
    submit(a5, memberId, SubmissionStatus.LATE);
    submit(a1, memberId, SubmissionStatus.LATE);
    submit(a2, memberId, SubmissionStatus.SUBMITTED);

    MeSummaryResult.Response result = meSummaryService.getSummary(memberId);

    assertThat(result.lateSubmittedLectures()).extracting(MeSummaryResult.LectureBrief::week)
        .containsExactly(1, 5);
  }

  @Test
  @DisplayName("profile 은 UserAccount 를 매핑하며 studentId 는 studentNumber 다")
  void profileMapsStudentNumberToStudentId() {
    MeSummaryResult.Response result = meSummaryService.getSummary(memberId);

    assertThat(result.profile().studentId()).isEqualTo("21");
    assertThat(result.profile().studentYear()).isEqualTo(3);
    assertThat(result.profile().major()).isEqualTo("경영학과");
  }

  @Test
  @DisplayName("요청자가 활성 기수 부원이 아니면 403")
  void forbiddenWhenRequesterNotInActiveGeneration() {
    Long outsiderId = member("outsider", 8).getId();

    assertThatThrownBy(() -> meSummaryService.getSummary(outsiderId))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", CommonErrorCode.FORBIDDEN);
  }
}
