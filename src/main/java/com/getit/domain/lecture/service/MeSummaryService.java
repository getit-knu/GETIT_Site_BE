package com.getit.domain.lecture.service;

import com.getit.domain.lecture.dto.MeSummaryResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeSummaryService {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
  private static final Comparator<Lecture> BY_WEEK_THEN_ID =
      Comparator.comparing(Lecture::getWeek).thenComparing(Lecture::getId);

  private final LectureRepository lectureRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final GenerationQueryService generationQueryService;
  private final UserAccountService userAccountService;

  public MeSummaryResult.Response getSummary(Long userId) {
    GenerationSummary active = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    UserAccount me = userAccountService.findActiveById(userId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    if (me.generationNo() == null || !me.generationNo().equals(active.generationNo())) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    List<Lecture> lectures = lectureRepository.findPublishedByGeneration(active.id());
    Map<Long, Lecture> lectureById = lectures.stream()
        .collect(Collectors.toMap(Lecture::getId, Function.identity()));

    List<Assignment> assignments = assignmentRepository.findAllByLectureIdIn(
        lectures.stream().map(Lecture::getId).toList());
    List<AssignmentSubmission> mySubmissions = assignmentSubmissionRepository.findAllByAssignmentIdInAndUserId(
        assignments.stream().map(Assignment::getId).toList(), userId);
    Set<Long> submittedAssignmentIds = mySubmissions.stream()
        .map(AssignmentSubmission::getAssignmentId)
        .collect(Collectors.toSet());
    Set<Long> lateAssignmentIds = mySubmissions.stream()
        .filter(submission -> submission.getStatus() == SubmissionStatus.LATE)
        .map(AssignmentSubmission::getAssignmentId)
        .collect(Collectors.toSet());

    LocalDateTime now = LocalDateTime.now(ZONE_SEOUL);
    List<MeSummaryResult.LectureBrief> notSubmittedLectures = assignments.stream()
        .filter(assignment -> assignment.getDeadline().isBefore(now))
        .filter(assignment -> !submittedAssignmentIds.contains(assignment.getId()))
        .map(assignment -> lectureById.get(assignment.getLectureId()))
        .sorted(BY_WEEK_THEN_ID)
        .map(this::toBrief)
        .toList();
    List<MeSummaryResult.LectureBrief> lateSubmittedLectures = assignments.stream()
        .filter(assignment -> lateAssignmentIds.contains(assignment.getId()))
        .map(assignment -> lectureById.get(assignment.getLectureId()))
        .sorted(BY_WEEK_THEN_ID)
        .map(this::toBrief)
        .toList();

    MeSummaryResult.Stats stats = new MeSummaryResult.Stats(
        lectures.size(), mySubmissions.size(), notSubmittedLectures.size(), lateSubmittedLectures.size());

    return new MeSummaryResult.Response(toProfile(me), stats, notSubmittedLectures, lateSubmittedLectures);
  }

  private MeSummaryResult.Profile toProfile(UserAccount user) {
    return new MeSummaryResult.Profile(
        user.name(), user.email(), user.college(), user.major(),
        user.studentNumber(), user.studentYear(), user.profileImageUrl());
  }

  private MeSummaryResult.LectureBrief toBrief(Lecture lecture) {
    return new MeSummaryResult.LectureBrief(lecture.getId(), lecture.getWeek(), lecture.getTitle());
  }
}
