package com.getit.domain.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
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
class LectureStatServiceImplTest {

  private static final int ACTIVE_GENERATION_NO = 9;

  @Autowired
  private LectureStatService lectureStatService;

  @Autowired
  private LectureRepository lectureRepository;

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

  private Long activeGenerationId;
  private Long trackId;
  private Long subCategoryId;

  @BeforeEach
  void setUp() {
    Generation generation = Generation.create(ACTIVE_GENERATION_NO, 2026);
    generation.activate();
    activeGenerationId = generationRepository.save(generation).getId();
    trackId = trackRepository.save(Track.create("SW", 1)).getId();
    subCategoryId = subCategoryRepository.save(SubCategory.create("WEB 기초", 1, trackId)).getId();
  }

  private Lecture lecture(int week, boolean published, Long generationId) {
    return lectureRepository.save(Lecture.create(
        week, week + "주차", null, null, null, null, published, generationId, trackId, subCategoryId, 1L));
  }

  private Assignment assignment(Long lectureId, LocalDateTime deadline) {
    return assignmentRepository.save(Assignment.create(
        lectureId, "과제", "설명", deadline, Set.of(SubmissionType.LINK), null));
  }

  private Long submit(Long assignmentId, Long userId) {
    return assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignmentId, userId, null, "https://github.com/a/b", null, SubmissionStatus.SUBMITTED,
        LocalDateTime.now())).getId();
  }

  @Nested
  @DisplayName("countUnEvaluatedSubmissions")
  class CountUnEvaluated {

    @Test
    @DisplayName("feedback 이 없는 제출물만 센다")
    void countsSubmissionsWithoutFeedback() {
      Long a = assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
      Long evaluated = submit(a, 1L);
      submit(a, 2L);
      feedbackRepository.save(Feedback.create(evaluated, 100L, "잘했어요"));

      assertThat(lectureStatService.countUnEvaluatedSubmissions(ACTIVE_GENERATION_NO)).isEqualTo(1);
    }

    @Test
    @DisplayName("비공개 강의 제출물은 제외한다")
    void excludesUnpublishedLectures() {
      Long a = assignment(lecture(1, false, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
      submit(a, 1L);

      assertThat(lectureStatService.countUnEvaluatedSubmissions(ACTIVE_GENERATION_NO)).isZero();
    }

    @Test
    @DisplayName("없는 기수면 0 이다")
    void zeroWhenGenerationMissing() {
      assertThat(lectureStatService.countUnEvaluatedSubmissions(99)).isZero();
    }
  }

  @Nested
  @DisplayName("findWeeklyStats")
  class FindWeeklyStats {

    @Test
    @DisplayName("과제 있는 공개 강의를 최근 주차부터 size 개 반환하고 제출 수를 채운다")
    void returnsRecentWeeksWithSubmittedCount() {
      Long a1 = assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
      Long a2 = assignment(lecture(2, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1)).getId();
      assignment(lecture(3, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1));
      lecture(4, true, activeGenerationId);
      submit(a1, 1L);
      submit(a2, 1L);
      submit(a2, 2L);

      var stats = lectureStatService.findWeeklyStats(ACTIVE_GENERATION_NO, null, 2);

      assertThat(stats).extracting(WeeklySubmissionStat::week).containsExactly(3, 2);
      assertThat(stats).extracting(WeeklySubmissionStat::submittedCount).containsExactly(0L, 2L);
    }

    @Test
    @DisplayName("과제 없는 강의는 제외한다")
    void excludesLecturesWithoutAssignment() {
      lecture(1, true, activeGenerationId);
      assignment(lecture(2, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1));

      var stats = lectureStatService.findWeeklyStats(ACTIVE_GENERATION_NO, null, 5);

      assertThat(stats).extracting(WeeklySubmissionStat::week).containsExactly(2);
    }

    @Test
    @DisplayName("trackId 를 주면 해당 트랙 강의만 반환한다")
    void filtersByTrackId() {
      assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1));
      Long otherTrackId = trackRepository.save(Track.create("기획", 2)).getId();
      Lecture other = lectureRepository.save(Lecture.create(
          2, "2주차", null, null, null, null, true, activeGenerationId, otherTrackId, null, 1L));
      assignment(other.getId(), LocalDateTime.now().plusDays(1));

      var stats = lectureStatService.findWeeklyStats(ACTIVE_GENERATION_NO, otherTrackId, 5);

      assertThat(stats).extracting(WeeklySubmissionStat::week).containsExactly(2);
    }

    @Test
    @DisplayName("size 가 0 이하면 빈 리스트다")
    void emptyWhenSizeNotPositive() {
      assignment(lecture(1, true, activeGenerationId).getId(), LocalDateTime.now().plusDays(1));

      assertThat(lectureStatService.findWeeklyStats(ACTIVE_GENERATION_NO, null, 0)).isEmpty();
    }
  }

  @Nested
  @DisplayName("findOngoingLectures")
  class FindOngoingLectures {

    @Test
    @DisplayName("마감이 안 지난 과제를 마감 임박순으로 반환한다")
    void returnsNotOverdueOrderedByDeadline() {
      Long later = assignment(lecture(1, true, activeGenerationId).getId(),
          LocalDateTime.now().plusDays(5)).getId();
      Long soon = assignment(lecture(2, true, activeGenerationId).getId(),
          LocalDateTime.now().plusDays(1)).getId();
      assignment(lecture(3, true, activeGenerationId).getId(), LocalDateTime.now().minusDays(1));
      submit(soon, 1L);

      var ongoing = lectureStatService.findOngoingLectures(ACTIVE_GENERATION_NO);

      assertThat(ongoing).extracting(OngoingLectureStat::lectureId)
          .containsExactly(assignmentLectureId(soon), assignmentLectureId(later));
      assertThat(ongoing.get(0).submittedCount()).isEqualTo(1L);
      assertThat(ongoing.get(0).subCategoryId()).isEqualTo(subCategoryId);
    }

    @Test
    @DisplayName("비공개 강의 과제는 제외한다")
    void excludesUnpublishedLectures() {
      assignment(lecture(1, false, activeGenerationId).getId(), LocalDateTime.now().plusDays(1));

      assertThat(lectureStatService.findOngoingLectures(ACTIVE_GENERATION_NO)).isEmpty();
    }
  }

  private Long assignmentLectureId(Long assignmentId) {
    return assignmentRepository.findById(assignmentId).orElseThrow().getLectureId();
  }
}
