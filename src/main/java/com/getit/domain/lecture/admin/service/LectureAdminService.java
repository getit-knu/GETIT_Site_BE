package com.getit.domain.lecture.admin.service;

import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.lecture.admin.dto.LectureRequest;
import com.getit.domain.lecture.admin.dto.LectureAdminResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.LectureFile;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureFileRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.service.CategoryQueryService;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.service.UserQueryService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureAdminService {

  private final LectureRepository lectureRepository;
  private final LectureFileRepository lectureFileRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final FeedbackRepository feedbackRepository;
  private final GenerationQueryService generationQueryService;
  private final CategoryQueryService categoryQueryService;
  private final UserQueryService userQueryService;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  @Transactional
  public Lecture createLecture(LectureRequest.Create request, Long createdBy) {
    Long generationId = resolveGenerationId(request.generationId());
    validateWritable(generationId);
    validateCategory(request.trackId(), request.subCategoryId());

    Lecture lecture = Lecture.create(
        request.week(), request.title(), request.description(), request.youtubeUrl(),
        request.materialUrl(), request.durationMinutes(), request.isPublishedOrDefault(),
        generationId, request.trackId(), request.subCategoryId(), createdBy);
    lectureRepository.save(lecture);

    connectFiles(lecture.getId(), request.fileIds());
    createAssignment(lecture.getId(), request.assignment());

    return lecture;
  }

  public LectureAdminResult.ListResult getLectures(Long generationId, Long trackId, Long subCategoryId) {
    Long resolvedGenerationId = resolveGenerationId(generationId);

    List<Lecture> lectures = lectureRepository.findAllByFilters(resolvedGenerationId, trackId, subCategoryId);
    List<Long> lectureIds = lectures.stream().map(Lecture::getId).toList();
    Map<Long, Assignment> assignmentsByLectureId = assignmentRepository.findAllByLectureIdIn(lectureIds).stream()
        .collect(Collectors.toMap(Assignment::getLectureId, Function.identity()));

    long totalCount = countActiveMembers(resolvedGenerationId);
    Map<Long, List<AssignmentSubmission>> submissionsByAssignmentId = findSubmissionsByAssignmentId(
        assignmentsByLectureId.values().stream().map(Assignment::getId).toList());
    Set<Long> feedbackDoneSubmissionIds = findFeedbackDoneSubmissionIds(submissionsByAssignmentId);

    List<LectureAdminResult.LectureCard> cards = lectures.stream()
        .map(lecture -> {
          Assignment assignment = assignmentsByLectureId.get(lecture.getId());
          List<AssignmentSubmission> submissions = assignment == null
              ? List.of() : submissionsByAssignmentId.getOrDefault(assignment.getId(), List.of());
          long feedbackDoneCount = submissions.stream()
              .filter(submission -> feedbackDoneSubmissionIds.contains(submission.getId()))
              .count();
          return LectureAdminResult.LectureCard.of(
              lecture, assignment, submissions.size(), totalCount, feedbackDoneCount);
        })
        .toList();

    return new LectureAdminResult.ListResult(categoryQueryService.findAllTracksWithSubCategories(), cards);
  }

  private long countActiveMembers(Long generationId) {
    Integer generationNo = generationQueryService.findById(generationId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.GENERATION_NOT_FOUND))
        .generationNo();
    return userQueryService.findActiveMembers(generationNo).size();
  }

  private Map<Long, List<AssignmentSubmission>> findSubmissionsByAssignmentId(List<Long> assignmentIds) {
    return assignmentSubmissionRepository.findAllByAssignmentIdIn(assignmentIds).stream()
        .collect(Collectors.groupingBy(AssignmentSubmission::getAssignmentId));
  }

  private Set<Long> findFeedbackDoneSubmissionIds(Map<Long, List<AssignmentSubmission>> submissionsByAssignmentId) {
    List<Long> submissionIds = submissionsByAssignmentId.values().stream()
        .flatMap(List::stream)
        .map(AssignmentSubmission::getId)
        .toList();
    return feedbackRepository.findSubmissionIdsWithFeedback(submissionIds);
  }

  public LectureAdminResult.DetailResult getLecture(Long lectureId) {
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));

    List<LectureFile> lectureFiles = lectureFileRepository.findAllByLectureIdOrderByIdAsc(lectureId);
    Map<Long, FileInfo> fileInfoByFileId = fileQueryService
        .findAllByIds(lectureFiles.stream().map(LectureFile::getFileId).toList()).stream()
        .collect(Collectors.toMap(FileInfo::fileId, Function.identity()));
    Map<Boolean, List<LectureFile>> lectureFilesByFileInfoFound = lectureFiles.stream()
        .collect(Collectors.partitioningBy(lectureFile -> fileInfoByFileId.containsKey(lectureFile.getFileId())));

    List<LectureFile> missingLectureFiles = lectureFilesByFileInfoFound.get(false);
    if (!missingLectureFiles.isEmpty()) {
      log.warn("강의자료 파일 조회 실패. lectureId={}, fileIds={}", lectureId,
          missingLectureFiles.stream().map(LectureFile::getFileId).toList());
    }
    List<LectureAdminResult.FileItem> files = lectureFilesByFileInfoFound.get(true).stream()
        .map(lectureFile -> LectureAdminResult.FileItem.of(
            lectureFile.getDisplayName(), fileInfoByFileId.get(lectureFile.getFileId())))
        .toList();

    LectureAdminResult.AssignmentResult assignment = assignmentRepository.findByLectureId(lectureId)
        .map(LectureAdminResult.AssignmentResult::from)
        .orElse(null);

    return LectureAdminResult.DetailResult.of(lecture, files, assignment);
  }

  @Transactional
  public LectureAdminResult.DetailResult updateLecture(Long lectureId, LectureRequest.Update request) {
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));

    // 지난 기수의 강의는 손대지 못한다. 옮겨 넣는 것도 막는다 — 그러면 지난 기수에
    // 새 자료가 생겨 아카이브가 아니게 된다.
    validateWritable(lecture.getGenerationId());
    Long generationId = request.generationId() != null
        ? resolveGenerationId(request.generationId())
        : lecture.getGenerationId();
    validateWritable(generationId);
    validateCategory(request.trackId(), request.subCategoryId());

    lecture.update(
        request.week(), request.title(), request.description(), request.youtubeUrl(),
        request.materialUrl(), request.durationMinutes(), request.isPublishedOrDefault(),
        generationId, request.trackId(), request.subCategoryId()
    );

    updateFiles(lectureId, request.fileIds());
    updateAssignment(lectureId, request.assignment());

    return getLecture(lectureId);
  }

  @Transactional
  public void deleteLecture(Long lectureId) {
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
    validateWritable(lecture.getGenerationId());
    lecture.delete();
  }

  /**
   * 지난 기수의 자료는 읽기 전용이다. (이슈 #168)
   *
   * <p>조회에는 걸지 않는다. {@code getLectures} 는 {@code generationId} 를 받아 지난 기수를
   * 열람하라고 만든 것이고, 그게 아카이브의 뜻이다. 막는 것은 쓰기뿐이다.
   *
   * <p>404 가 아니라 409 를 준다. 어드민은 지난 기수를 계속 볼 수 있으므로, 없는 것처럼
   * 감추는 것보다 "볼 수는 있지만 고칠 수 없다"가 맞다. 부원용 {@code SubmissionService} 가
   * 404 를 주는 것은 부원에게 애초에 보이지 않는 자료이기 때문이다.
   */
  private void validateWritable(Long generationId) {
    Long activeGenerationId = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ACTIVE_GENERATION_NOT_FOUND))
        .id();
    if (!generationId.equals(activeGenerationId)) {
      throw new BusinessException(LectureErrorCode.PAST_GENERATION_READ_ONLY);
    }
  }

  private Long resolveGenerationId(Long requestedGenerationId) {
    if (requestedGenerationId == null) {
      return generationQueryService.findActive()
          .orElseThrow(() -> new BusinessException(LectureErrorCode.ACTIVE_GENERATION_NOT_FOUND))
          .id();
    }
    return generationQueryService.findById(requestedGenerationId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.GENERATION_NOT_FOUND))
        .id();
  }

  private void validateCategory(Long trackId, Long subCategoryId) {
    if (!categoryQueryService.existsTrack(trackId)) {
      throw new BusinessException(LectureErrorCode.TRACK_NOT_FOUND);
    }
    if (subCategoryId == null) {
      return;
    }

    Long parentTrackId = categoryQueryService.findTrackIdOfSubCategory(subCategoryId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.SUBCATEGORY_NOT_FOUND));
    if (!parentTrackId.equals(trackId)) {
      throw new BusinessException(LectureErrorCode.SUBCATEGORY_TRACK_MISMATCH);
    }
  }

  private void connectFiles(Long lectureId, List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();

    Map<Long, FileInfo> fileInfoByFileId = fileQueryService.findAllByIds(distinctFileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, Function.identity()));
    if (fileInfoByFileId.size() != distinctFileIds.size()) {
      List<Long> missingFileIds = distinctFileIds.stream()
          .filter(fileId -> !fileInfoByFileId.containsKey(fileId))
          .toList();
      throw new BusinessException(FileErrorCode.FILE_NOT_FOUND, "파일을 찾을 수 없습니다: " + missingFileIds);
    }

    fileConnectionService.connectAll(distinctFileIds);
    List<LectureFile> lectureFiles = distinctFileIds.stream()
        .map(fileId -> LectureFile.create(fileInfoByFileId.get(fileId).originalName(), lectureId, fileId))
        .toList();
    lectureFileRepository.saveAll(lectureFiles);
  }

  private void createAssignment(Long lectureId, LectureRequest.AssignmentPart assignment) {
    if (assignment == null) {
      return;
    }
    assignmentRepository.save(Assignment.create(
        lectureId, assignment.title(), assignment.description(), assignment.deadline(),
        assignment.allowedTypes(), assignment.linkPlaceholder()));
  }

  private void updateFiles(Long lectureId, List<Long> fileIds) {
    List<LectureFile> existingFiles = lectureFileRepository.findAllByLectureIdOrderByIdAsc(lectureId);
    Set<Long> existingFileIds = existingFiles.stream().map(LectureFile::getFileId).collect(Collectors.toSet());
    Set<Long> requestedFileIds = fileIds == null
        ? Set.of()
        : fileIds.stream().collect(Collectors.toSet());

    List<LectureFile> filesToRemove = existingFiles.stream()
        .filter(lectureFile -> !requestedFileIds.contains(lectureFile.getFileId()))
        .toList();
    List<Long> fileIdsToAdd = requestedFileIds.stream()
        .filter(fileId -> !existingFileIds.contains(fileId))
        .toList();

    fileConnectionService.disconnectAll(filesToRemove.stream().map(LectureFile::getFileId).toList());
    lectureFileRepository.deleteAll(filesToRemove);

    connectFiles(lectureId, fileIdsToAdd);
  }

  private void updateAssignment(Long lectureId, LectureRequest.AssignmentPart assignmentPart) {
    Optional<Assignment> existing = assignmentRepository.findByLectureId(lectureId);
    if (assignmentPart == null) {
      existing.ifPresent(assignmentRepository::delete);
      return;
    }
    if (existing.isPresent()) {
      existing.get().update(
          assignmentPart.title(), assignmentPart.description(), assignmentPart.deadline(),
          assignmentPart.allowedTypes(), assignmentPart.linkPlaceholder());
      return;
    }
    createAssignment(lectureId, assignmentPart);
  }
}
