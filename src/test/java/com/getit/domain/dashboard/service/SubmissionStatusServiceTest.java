package com.getit.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.dashboard.dto.SubmissionStatusResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.entity.SubmissionType;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SubmissionStatusServiceTest {

  @Autowired
  private SubmissionStatusService submissionStatusService;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private LectureRepository lectureRepository;

  @Autowired
  private AssignmentRepository assignmentRepository;

  @Autowired
  private AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Autowired
  private UserRepository userRepository;

  private Long activeGenerationId() {
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    return generationRepository.save(generation).getId();
  }

  private void member(String providerId) {
    User user = User.createGuest(providerId, providerId + "@getit.com", "김부원", null);
    user.promoteToMember(9);
    userRepository.save(user);
  }

  @Test
  @DisplayName("최근 주차 제출 현황과 전체 부원 수를 반환한다")
  void returnsWeeklyStatsWithTotalMemberCount() {
    Long generationId = activeGenerationId();
    member("google-sub-40");
    member("google-sub-41");

    Lecture lecture = lectureRepository.save(Lecture.create(
        1, "1주차", null, null, null, null, true, generationId, null, null, 1L));
    Assignment assignment = assignmentRepository.save(Assignment.create(
        lecture.getId(), "과제", "설명", LocalDateTime.now().plusDays(1), Set.of(SubmissionType.LINK), null));
    assignmentSubmissionRepository.save(AssignmentSubmission.submit(
        assignment.getId(), 1L, null, "https://github.com/a/b", null, SubmissionStatus.SUBMITTED,
        LocalDateTime.now()));

    SubmissionStatusResult result = submissionStatusService.getSubmissionStatus(null, 5);

    assertThat(result.totalMemberCount()).isEqualTo(2);
    assertThat(result.weeks()).hasSize(1);
    assertThat(result.weeks().get(0).submittedCount()).isEqualTo(1);
    assertThat(result.weeks().get(0).totalCount()).isEqualTo(2);
    assertThat(result.weeks().get(0).rate()).isEqualTo(50.0);
  }

  @Test
  @DisplayName("활성 기수가 없으면 totalMemberCount 0, weeks 빈 리스트다")
  void returnsEmptyWhenNoActiveGeneration() {
    SubmissionStatusResult result = submissionStatusService.getSubmissionStatus(null, 5);

    assertThat(result.totalMemberCount()).isZero();
    assertThat(result.weeks()).isEmpty();
  }
}
