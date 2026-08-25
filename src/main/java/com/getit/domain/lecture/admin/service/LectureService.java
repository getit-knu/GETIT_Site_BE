package com.getit.domain.lecture.admin.service;

import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.service.FileConnectionService;
import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.lecture.admin.dto.LectureRequest;
import com.getit.domain.lecture.admin.dto.LectureResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.LectureFile;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.LectureFileRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.setting.category.service.CategoryQueryService;
import com.getit.domain.setting.generation.service.GenerationQueryService;
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
public class LectureService {

  private final LectureRepository lectureRepository;
  private final LectureFileRepository lectureFileRepository;
  private final AssignmentRepository assignmentRepository;
  private final GenerationQueryService generationQueryService;
  private final CategoryQueryService categoryQueryService;
  private final FileQueryService fileQueryService;
  private final FileConnectionService fileConnectionService;

  @Transactional
  public Lecture createLecture(LectureRequest.Create request, Long createdBy) {
    Long generationId = resolveGenerationId(request.generationId());
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

  public LectureResult.ListResult getLectures(Long generationId, Long trackId, Long subCategoryId) {
    Long resolvedGenerationId = resolveGenerationId(generationId);

    List<Lecture> lectures = lectureRepository.findAllByFilters(resolvedGenerationId, trackId, subCategoryId);
    List<Long> lectureIds = lectures.stream().map(Lecture::getId).toList();
    Map<Long, Assignment> assignmentsByLectureId = assignmentRepository.findAllByLectureIdIn(lectureIds).stream()
        .collect(Collectors.toMap(Assignment::getLectureId, Function.identity()));

    List<LectureResult.LectureCard> cards = lectures.stream()
        .map(lecture -> LectureResult.LectureCard.of(lecture, assignmentsByLectureId.get(lecture.getId())))
        .toList();

    return new LectureResult.ListResult(categoryQueryService.findAllTracksWithSubCategories(), cards);
  }

  public LectureResult.DetailResult getLecture(Long lectureId) {
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
    List<LectureResult.FileItem> files = lectureFilesByFileInfoFound.get(true).stream()
        .map(lectureFile -> LectureResult.FileItem.of(
            lectureFile.getDisplayName(), fileInfoByFileId.get(lectureFile.getFileId())))
        .toList();

    LectureResult.AssignmentResult assignment = assignmentRepository.findByLectureId(lectureId)
        .map(LectureResult.AssignmentResult::from)
        .orElse(null);

    return LectureResult.DetailResult.of(lecture, files, assignment);
  }

  @Transactional
  public LectureResult.DetailResult updateLecture(Long lectureId, LectureRequest.Update request) {
    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));

    Long generationId = request.generationId() != null
        ? resolveGenerationId(request.generationId())
        : lecture.getGenerationId();
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
    lecture.delete();
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
        : fileIds.stream().distinct().collect(Collectors.toSet());

    List<LectureFile> filesToRemove = existingFiles.stream()
        .filter(lectureFile -> !requestedFileIds.contains(lectureFile.getFileId()))
        .toList();
    List<Long> fileIdsToAdd = requestedFileIds.stream()
        .filter(fileId -> !existingFileIds.contains(fileId))
        .toList();

    filesToRemove.forEach(lectureFile -> fileConnectionService.disconnect(lectureFile.getFileId()));
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
