package com.getit.domain.lecture.service;

import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository.AssignmentSubmissionCount;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.lecture.repository.LectureRepository.OngoingLectureRow;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class LectureStatServiceImpl implements LectureStatService {

  private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

  private final GenerationQueryService generationQueryService;
  private final LectureRepository lectureRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;

  @Override
  public long countUnEvaluatedSubmissions(int generationNo) {
    Long generationId = resolveGenerationId(generationNo).orElse(null);
    if (generationId == null) {
      return 0L;
    }
    List<Long> lectureIds = lectureRepository.findPublishedLectureIds(generationId);
    if (lectureIds.isEmpty()) {
      return 0L;
    }
    return assignmentSubmissionRepository.countUnEvaluatedInLectures(lectureIds);
  }

  @Override
  public List<WeeklySubmissionStat> findWeeklyStats(int generationNo, Long trackId, int size) {
    Long generationId = resolveGenerationId(generationNo).orElse(null);
    if (generationId == null || size <= 0) {
      return List.of();
    }
    List<Lecture> lectures = lectureRepository.findRecentPublishedWithAssignment(
        generationId, trackId, PageRequest.of(0, size));
    Map<Long, Assignment> assignmentByLectureId = assignmentByLectureId(lectures);
    Map<Long, Long> submittedByAssignmentId = submittedCountByAssignmentIds(
        assignmentByLectureId.values().stream().map(Assignment::getId).toList());

    return lectures.stream()
        .map(lecture -> {
          Assignment assignment = assignmentByLectureId.get(lecture.getId());
          long submitted = assignment == null
              ? 0L
              : submittedByAssignmentId.getOrDefault(assignment.getId(), 0L);
          return new WeeklySubmissionStat(lecture.getId(), lecture.getWeek(), lecture.getTitle(), submitted);
        })
        .toList();
  }

  @Override
  public List<OngoingLectureStat> findOngoingLectures(int generationNo) {
    Long generationId = resolveGenerationId(generationNo).orElse(null);
    if (generationId == null) {
      return List.of();
    }
    LocalDateTime todayStart = LocalDate.now(ZONE_SEOUL).atStartOfDay();
    List<OngoingLectureRow> rows = lectureRepository.findOngoingWithAssignment(generationId, todayStart);
    if (rows.isEmpty()) {
      return List.of();
    }
    Map<Long, Long> submittedByAssignmentId = submittedCountByAssignmentIds(
        rows.stream().map(OngoingLectureRow::getAssignmentId).toList());

    return rows.stream()
        .map(row -> new OngoingLectureStat(
            row.getLectureId(),
            row.getTitle(),
            row.getSubCategoryId(),
            row.getDeadline(),
            submittedByAssignmentId.getOrDefault(row.getAssignmentId(), 0L)))
        .toList();
  }

  private Optional<Long> resolveGenerationId(int generationNo) {
    return generationQueryService.findByGenerationNo(generationNo).map(GenerationSummary::id);
  }

  private Map<Long, Assignment> assignmentByLectureId(List<Lecture> lectures) {
    List<Long> lectureIds = lectures.stream().map(Lecture::getId).toList();
    return assignmentRepository.findAllByLectureIdIn(lectureIds).stream()
        .collect(Collectors.toMap(Assignment::getLectureId, Function.identity(), (first, ignored) -> first));
  }

  private Map<Long, Long> submittedCountByAssignmentIds(List<Long> assignmentIds) {
    if (assignmentIds.isEmpty()) {
      return Map.of();
    }
    return assignmentSubmissionRepository.countByAssignmentIdsGrouped(assignmentIds).stream()
        .collect(Collectors.toMap(
            AssignmentSubmissionCount::getAssignmentId, AssignmentSubmissionCount::getCount));
  }
}
