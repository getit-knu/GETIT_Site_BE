package com.getit.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.dashboard.dto.OngoingLectureResult;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OngoingLectureServiceTest {

  @Autowired
  private OngoingLectureService ongoingLectureService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private TrackRepository trackRepository;

  @Autowired
  private SubCategoryRepository subCategoryRepository;

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("진행 중 강의를 subCategoryName · 제출 현황과 함께 반환한다")
  void returnsOngoingLecturesWithSubCategoryNameAndCounts() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    Long generationId = generationRepository.save(generation).getId();
    Long trackId = trackRepository.save(Track.create("창업", 1)).getId();
    Long subCategoryId = subCategoryRepository.save(SubCategory.create("창업 빌드업", 1, trackId)).getId();

    User member = User.createGuest("google-sub-50", "member50@getit.com", "김부원", null);
    member.promoteToMember(9);
    userRepository.save(member);

    LocalDateTime deadline = LocalDateTime.now().plusDays(3);
    Lecture lecture = lectureRepository.save(Lecture.create(
        4, "창업 빌드업 4차시", null, null, null, null, true, generationId, trackId, subCategoryId, 1L));
    Assignment assignment = assignmentRepository.save(Assignment.create(
        lecture.getId(), "과제", "설명", deadline, Set.of(SubmissionType.LINK), null));
    assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignment.getId(), member.getId(), null, "https://github.com/a/b", null,
        SubmissionStatus.SUBMITTED, LocalDateTime.now()));

    List<OngoingLectureResult> results = ongoingLectureService.getOngoingLectures();

    assertThat(results).hasSize(1);
    OngoingLectureResult result = results.get(0);
    assertThat(result.title()).isEqualTo("창업 빌드업 4차시");
    assertThat(result.subCategoryName()).isEqualTo("창업 빌드업");
    assertThat(result.deadline()).isEqualTo(deadline.toLocalDate());
    assertThat(result.submittedCount()).isEqualTo(1);
    assertThat(result.totalCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("활성 기수가 없으면 빈 리스트다")
  void returnsEmptyWhenNoActiveGeneration() {
    assertThat(ongoingLectureService.getOngoingLectures()).isEmpty();
  }
}
