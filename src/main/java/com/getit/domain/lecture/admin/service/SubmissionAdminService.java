package com.getit.domain.lecture.admin.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.lecture.admin.dto.SubmissionDetailResult;
import com.getit.domain.lecture.admin.dto.SubmissionOverviewResult;
import com.getit.domain.lecture.admin.dto.SubmissionOverviewResult.Row;
import com.getit.domain.lecture.util.KstDateTimes;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.SubmissionStatus;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.MemberSummary;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.domain.user.service.UserQueryService;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubmissionAdminService {

  private static final Set<String> PREVIEWABLE_CONTENT_TYPES = Set.of("application/pdf");

  private final LectureRepository lectureRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final FeedbackRepository feedbackRepository;
  private final GenerationQueryService generationQueryService;
  private final UserQueryService userQueryService;
  private final UserAccountService userAccountService;
  private final FileQueryService fileQueryService;

  private record RowCandidate(
      Long userId,
      String userName,
      String major,
      Long groupId,
      Long submissionId,
      boolean submitted,
      SubmissionStatus status,
      LocalDateTime submittedAt,
      boolean feedbackDone
  ) {

    Row toRow() {
      return new Row(
          userId, userName, major, submissionId, submitted, status,
          KstDateTimes.toOffset(submittedAt), feedbackDone);
    }
  }

  public SubmissionOverviewResult.Overview getOverview(
      Long lectureId, Boolean submittedFilter, Boolean feedbackDoneFilter, Long groupId, Pageable pageable) {
    Lecture lecture = findLecture(lectureId);
    Assignment assignment = findAssignmentByLectureId(lectureId);

    List<RowCandidate> allCandidates = buildCandidates(lecture, assignment);
    SubmissionOverviewResult.Counts counts = countOf(allCandidates);

    List<Row> filteredRows = filterCandidates(allCandidates, submittedFilter, feedbackDoneFilter, groupId).stream()
        .map(RowCandidate::toRow)
        .toList();
    Page<Row> page = paginate(filteredRows, pageable);

    SubmissionOverviewResult.LectureSummary lectureSummary = SubmissionOverviewResult.LectureSummary.of(
        lecture.getId(), lecture.getTitle(), assignment.getDeadline());
    return SubmissionOverviewResult.Overview.of(lectureSummary, counts, page);
  }

  public SubmissionDetailResult.Detail getDetail(Long submissionId) {
    AssignmentSubmission submission = findSubmission(submissionId);
    Assignment assignment = findAssignment(submission.getAssignmentId());
    Lecture lecture = findLecture(assignment.getLectureId());
    UserAccount user = findUser(submission.getUserId());

    SubmissionDetailResult.FileSummary fileSummary = submission.getFileId() == null
        ? null : toFileSummary(submission.getFileId());
    List<Feedback> feedbackEntities = feedbackRepository.findAllBySubmissionIdOrderByIdAsc(submissionId);
    Map<Long, String> adminNameById = feedbackEntities.stream()
        .map(Feedback::getAdminId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), adminId -> findUser(adminId).name()));
    List<SubmissionDetailResult.FeedbackItem> feedbacks = feedbackEntities.stream()
        .map(feedback -> SubmissionDetailResult.FeedbackItem.of(feedback, adminNameById.get(feedback.getAdminId())))
        .toList();

    List<RowCandidate> submittedCandidates = buildCandidates(lecture, assignment).stream()
        .filter(RowCandidate::submitted)
        .toList();
    SubmissionDetailResult.Navigation navigation = navigationOf(submittedCandidates, submissionId);

    return new SubmissionDetailResult.Detail(
        submission.getId(),
        new SubmissionDetailResult.LectureSummary(lecture.getId(), lecture.getTitle()),
        new SubmissionDetailResult.UserSummary(user.id(), user.name(), user.major()),
        fileSummary,
        submission.getLinkUrl(),
        submission.getComment(),
        KstDateTimes.toOffset(submission.getSubmittedAt()),
        submission.getStatus(),
        feedbacks,
        navigation);
  }

  public SubmissionDetailResult.Navigation navigate(
      Long lectureId, Long currentSubmissionId, Boolean submittedFilter, Boolean feedbackDoneFilter, Long groupId) {
    Lecture lecture = findLecture(lectureId);
    Assignment assignment = findAssignmentByLectureId(lectureId);
    List<RowCandidate> candidates = filterCandidates(
        buildCandidates(lecture, assignment), submittedFilter, feedbackDoneFilter, groupId).stream()
        .filter(RowCandidate::submitted)
        .toList();
    return navigationOf(candidates, currentSubmissionId);
  }

  private List<RowCandidate> buildCandidates(Lecture lecture, Assignment assignment) {
    Integer generationNo = generationQueryService.findById(lecture.getGenerationId())
        .orElseThrow(() -> new BusinessException(LectureErrorCode.GENERATION_NOT_FOUND))
        .generationNo();
    List<MemberSummary> members = userQueryService.findActiveMembers(generationNo);

    List<AssignmentSubmission> submissions =
        assignmentSubmissionRepository.findAllByAssignmentId(assignment.getId());
    Map<Long, AssignmentSubmission> submissionByUserId = submissions.stream()
        .collect(Collectors.toMap(AssignmentSubmission::getUserId, Function.identity()));

    List<Long> submissionIds = submissions.stream().map(AssignmentSubmission::getId).toList();
    Set<Long> feedbackDoneSubmissionIds = feedbackRepository.findAllBySubmissionIdIn(submissionIds).stream()
        .map(Feedback::getSubmissionId)
        .collect(Collectors.toSet());

    return members.stream()
        .map(member -> toCandidate(member, submissionByUserId.get(member.userId()), feedbackDoneSubmissionIds))
        .sorted(Comparator.comparing(RowCandidate::userId))
        .toList();
  }

  private RowCandidate toCandidate(
      MemberSummary member, AssignmentSubmission submission, Set<Long> feedbackDoneSubmissionIds) {
    if (submission == null) {
      return new RowCandidate(
          member.userId(), member.userName(), member.major(), member.groupId(),
          null, false, null, null, false);
    }
    return new RowCandidate(
        member.userId(), member.userName(), member.major(), member.groupId(),
        submission.getId(), true, submission.getStatus(), submission.getSubmittedAt(),
        feedbackDoneSubmissionIds.contains(submission.getId()));
  }

  private List<RowCandidate> filterCandidates(
      List<RowCandidate> candidates, Boolean submittedFilter, Boolean feedbackDoneFilter, Long groupId) {
    return candidates.stream()
        .filter(row -> submittedFilter == null || row.submitted() == submittedFilter)
        .filter(row -> feedbackDoneFilter == null || row.feedbackDone() == feedbackDoneFilter)
        .filter(row -> groupId == null || groupId.equals(row.groupId()))
        .toList();
  }

  private SubmissionOverviewResult.Counts countOf(List<RowCandidate> candidates) {
    long submitted = candidates.stream().filter(RowCandidate::submitted).count();
    return new SubmissionOverviewResult.Counts(submitted, candidates.size() - submitted, candidates.size());
  }

  private Page<Row> paginate(List<Row> rows, Pageable pageable) {
    int start = (int) pageable.getOffset();
    if (start >= rows.size()) {
      return new PageImpl<>(List.of(), pageable, rows.size());
    }
    int end = Math.min(start + pageable.getPageSize(), rows.size());
    return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
  }

  private SubmissionDetailResult.Navigation navigationOf(List<RowCandidate> submittedCandidates, Long currentId) {
    int index = -1;
    for (int i = 0; i < submittedCandidates.size(); i++) {
      if (submittedCandidates.get(i).submissionId().equals(currentId)) {
        index = i;
        break;
      }
    }
    Long prevId = index > 0 ? submittedCandidates.get(index - 1).submissionId() : null;
    Long nextId = index >= 0 && index < submittedCandidates.size() - 1
        ? submittedCandidates.get(index + 1).submissionId() : null;
    return new SubmissionDetailResult.Navigation(currentId, submittedCandidates.size(), prevId, nextId);
  }

  private SubmissionDetailResult.FileSummary toFileSummary(Long fileId) {
    FileInfo fileInfo = fileQueryService.findById(fileId);
    boolean previewable = fileInfo.contentType() != null
        && (fileInfo.contentType().startsWith("image/")
            || PREVIEWABLE_CONTENT_TYPES.contains(fileInfo.contentType()));
    return new SubmissionDetailResult.FileSummary(
        fileInfo.fileId(), fileInfo.originalName(), fileInfo.url(),
        previewable ? fileInfo.url() : null, fileInfo.contentType(), fileInfo.size(), previewable);
  }

  private Lecture findLecture(Long lectureId) {
    return lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
  }

  private Assignment findAssignmentByLectureId(Long lectureId) {
    return assignmentRepository.findByLectureId(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ASSIGNMENT_NOT_FOUND));
  }

  private Assignment findAssignment(Long assignmentId) {
    return assignmentRepository.findById(assignmentId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ASSIGNMENT_NOT_FOUND));
  }

  private AssignmentSubmission findSubmission(Long submissionId) {
    return assignmentSubmissionRepository.findById(submissionId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));
  }

  private UserAccount findUser(Long userId) {
    return userAccountService.findActiveById(userId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBMISSION_NOT_FOUND));
  }
}
