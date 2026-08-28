package com.getit.domain.lecture.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.lecture.dto.LectureResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Feedback;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.entity.LectureFile;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
import com.getit.domain.lecture.repository.FeedbackRepository;
import com.getit.domain.lecture.repository.LectureFileRepository;
import com.getit.domain.lecture.repository.LectureRepository;
import com.getit.domain.lecture.repository.LectureRepository.SubCategoryLectureCount;
import com.getit.domain.lecture.util.KstDateTimes;
import com.getit.domain.setting.category.dto.CategorySummary;
import com.getit.domain.setting.category.service.CategoryQueryService;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.service.UserAccountService;
import com.getit.global.exception.BusinessException;
import com.getit.global.exception.CommonErrorCode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부원 강의 조회. (API 명세서 4.1 ~ 4.3)
 *
 * <p>공개(published) 강의만, 요청자가 속한 활성 기수 범위에서만 노출한다. 관리자 조회는
 * {@code admin.service.LectureAdminService} 를 따로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

  private final LectureRepository lectureRepository;
  private final LectureFileRepository lectureFileRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final FeedbackRepository feedbackRepository;
  private final GenerationQueryService generationQueryService;
  private final CategoryQueryService categoryQueryService;
  private final UserAccountService userAccountService;
  private final FileQueryService fileQueryService;

  public LectureResult.ListResult getLectures(
      Long userId, Long trackId, Long subCategoryId, Pageable pageable) {
    GenerationSummary generation = requireActiveMember(userId);

    Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Lecture> lectures =
        lectureRepository.findPublishedPage(generation.id(), trackId, subCategoryId, pageOnly);

    List<CategorySummary> tracks = categoryQueryService.findAllTracksWithSubCategories();
    CategoryNames names = CategoryNames.from(tracks);

    Map<Long, Assignment> assignmentByLectureId = assignmentRepository
        .findAllByLectureIdIn(lectures.stream().map(Lecture::getId).toList()).stream()
        .collect(Collectors.toMap(Assignment::getLectureId, Function.identity()));
    Set<Long> submittedAssignmentIds = findSubmittedAssignmentIds(userId, assignmentByLectureId.values());

    Page<LectureResult.Content> content = lectures.map(lecture -> {
      Assignment assignment = assignmentByLectureId.get(lecture.getId());
      return new LectureResult.Content(
          lecture.getId(),
          lecture.getWeek(),
          lecture.getTitle(),
          names.subCategoryNameOf(lecture),
          names.trackNameOf(lecture),
          lecture.getDurationMinutes(),
          assignment != null ? KstDateTimes.toOffset(assignment.getDeadline()) : null,
          assignment != null && submittedAssignmentIds.contains(assignment.getId()));
    });

    return LectureResult.ListResult.of(buildTabs(tracks, generation.id()), content);
  }

  public LectureResult.DetailResult getLecture(Long userId, Long lectureId) {
    GenerationSummary generation = requireActiveMember(userId);

    Lecture lecture = lectureRepository.findByIdAndDeletedAtIsNull(lectureId)
        .orElseThrow(() -> new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND));
    if (!lecture.isPublished() || !lecture.getGenerationId().equals(generation.id())) {
      throw new BusinessException(LectureErrorCode.LECTURE_NOT_FOUND);
    }

    CategoryNames names = CategoryNames.from(categoryQueryService.findAllTracksWithSubCategories());
    Assignment assignment = assignmentRepository.findByLectureId(lectureId).orElse(null);

    return new LectureResult.DetailResult(
        lecture.getId(),
        lecture.getWeek(),
        lecture.getTitle(),
        lecture.getDescription(),
        names.trackNameOf(lecture),
        names.subCategoryNameOf(lecture),
        lecture.getDurationMinutes(),
        lecture.getYoutubeUrl(),
        lecture.getMaterialUrl(),
        resolveAuthor(lecture.getCreatedBy()),
        KstDateTimes.toOffset(lecture.getCreatedAt()),
        resolveMaterials(lectureId),
        assignment == null ? null : new LectureResult.AssignmentInfo(
            assignment.getId(), assignment.getTitle(), assignment.getDescription(),
            KstDateTimes.toOffset(assignment.getDeadline())),
        assignment == null ? null : resolveMySubmission(assignment.getId(), userId));
  }

  private Set<Long> findSubmittedAssignmentIds(Long userId, Collection<Assignment> assignments) {
    List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
    return assignmentSubmissionRepository.findAllByAssignmentIdInAndUserId(assignmentIds, userId).stream()
        .map(AssignmentSubmission::getAssignmentId)
        .collect(Collectors.toSet());
  }

  private List<LectureResult.Tab> buildTabs(List<CategorySummary> tracks, Long generationId) {
    Map<Long, Long> countBySubCategoryId = lectureRepository
        .countPublishedBySubCategoryGrouped(generationId).stream()
        .collect(Collectors.toMap(
            SubCategoryLectureCount::getSubCategoryId, SubCategoryLectureCount::getCount));

    List<LectureResult.Tab> tabs = new ArrayList<>();
    for (CategorySummary track : tracks) {
      for (CategorySummary.SubCategoryBrief sub : track.subCategories()) {
        Long count = countBySubCategoryId.get(sub.id());
        if (count != null && count > 0) {
          tabs.add(new LectureResult.Tab(sub.id(), sub.name(), count));
        }
      }
    }
    return tabs;
  }

  private LectureResult.Author resolveAuthor(Long authorId) {
    return userAccountService.findActiveById(authorId)
        .map(user -> new LectureResult.Author(user.name(), user.profileImageUrl()))
        .orElse(new LectureResult.Author("UNKNOWN", null));
  }

  private List<LectureResult.Material> resolveMaterials(Long lectureId) {
    List<LectureFile> lectureFiles = lectureFileRepository.findAllByLectureIdOrderByIdAsc(lectureId);
    Map<Long, FileInfo> fileInfoByFileId = fileQueryService
        .findAllByIds(lectureFiles.stream().map(LectureFile::getFileId).toList()).stream()
        .collect(Collectors.toMap(FileInfo::fileId, Function.identity()));

    List<Long> missing = lectureFiles.stream()
        .map(LectureFile::getFileId)
        .filter(fileId -> !fileInfoByFileId.containsKey(fileId))
        .toList();
    if (!missing.isEmpty()) {
      log.warn("강의자료 파일 조회 실패. lectureId={}, fileIds={}", lectureId, missing);
    }

    return lectureFiles.stream()
        .filter(lectureFile -> fileInfoByFileId.containsKey(lectureFile.getFileId()))
        .map(lectureFile -> {
          FileInfo info = fileInfoByFileId.get(lectureFile.getFileId());
          return new LectureResult.Material(
              info.fileId(), lectureFile.getDisplayName(), info.size(), info.contentType());
        })
        .toList();
  }

  private LectureResult.MySubmission resolveMySubmission(Long assignmentId, Long userId) {
    AssignmentSubmission submission = assignmentSubmissionRepository
        .findByAssignmentIdAndUserId(assignmentId, userId).orElse(null);
    if (submission == null) {
      return null;
    }

    String fileUrl = null;
    String fileName = null;
    if (submission.getFileId() != null) {
      FileInfo info = fileQueryService.findAllByIds(List.of(submission.getFileId())).stream()
          .filter(file -> Objects.equals(file.fileId(), submission.getFileId()))
          .findFirst()
          .orElse(null);
      if (info == null) {
        log.warn("과제 제출 파일 조회 실패. submissionId={}, fileId={}",
            submission.getId(), submission.getFileId());
      } else {
        fileUrl = info.url();
        fileName = info.originalName();
      }
    }

    return new LectureResult.MySubmission(
        submission.getId(), fileUrl, fileName, submission.getLinkUrl(), submission.getComment(),
        KstDateTimes.toOffset(submission.getSubmittedAt()), submission.getStatus(),
        resolveFeedbacks(submission.getId()));
  }

  private List<LectureResult.FeedbackItem> resolveFeedbacks(Long submissionId) {
    List<Feedback> feedbacks = feedbackRepository.findAllBySubmissionIdOrderByIdAsc(submissionId);
    Map<Long, String> adminNameById = feedbacks.stream()
        .map(Feedback::getAdminId)
        .distinct()
        .collect(Collectors.toMap(Function.identity(), this::resolveAdminName));
    return feedbacks.stream()
        .map(feedback -> new LectureResult.FeedbackItem(
            feedback.getId(), adminNameById.get(feedback.getAdminId()), feedback.getContent(),
            KstDateTimes.toOffset(feedback.getCreatedAt())))
        .toList();
  }

  private String resolveAdminName(Long adminId) {
    return userAccountService.findActiveById(adminId).map(UserAccount::name).orElse("UNKNOWN");
  }

  private GenerationSummary requireActiveMember(Long userId) {
    GenerationSummary active = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(LectureErrorCode.ACTIVE_GENERATION_NOT_FOUND));
    UserAccount me = userAccountService.findActiveById(userId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));
    if (me.generationNo() == null || !me.generationNo().equals(active.generationNo())) {
      throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }
    return active;
  }

  private record CategoryNames(
      Map<Long, String> subCategoryName,
      Map<Long, String> trackNameBySubCategoryId,
      Map<Long, String> trackName
  ) {

    static CategoryNames from(List<CategorySummary> tracks) {
      Map<Long, String> subCategoryName = new HashMap<>();
      Map<Long, String> trackNameBySubCategoryId = new HashMap<>();
      Map<Long, String> trackName = new HashMap<>();
      for (CategorySummary track : tracks) {
        trackName.put(track.id(), track.name());
        for (CategorySummary.SubCategoryBrief sub : track.subCategories()) {
          subCategoryName.put(sub.id(), sub.name());
          trackNameBySubCategoryId.put(sub.id(), track.name());
        }
      }
      return new CategoryNames(subCategoryName, trackNameBySubCategoryId, trackName);
    }

    String subCategoryNameOf(Lecture lecture) {
      return lecture.getSubCategoryId() != null ? subCategoryName.get(lecture.getSubCategoryId()) : null;
    }

    String trackNameOf(Lecture lecture) {
      return lecture.getSubCategoryId() != null
          ? trackNameBySubCategoryId.get(lecture.getSubCategoryId())
          : trackName.get(lecture.getTrackId());
    }
  }
}
