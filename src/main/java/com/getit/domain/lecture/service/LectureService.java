package com.getit.domain.lecture.service;

import com.getit.domain.lecture.dto.LectureResult;
import com.getit.domain.lecture.entity.Assignment;
import com.getit.domain.lecture.entity.AssignmentSubmission;
import com.getit.domain.lecture.entity.Lecture;
import com.getit.domain.lecture.exception.LectureErrorCode;
import com.getit.domain.lecture.repository.AssignmentRepository;
import com.getit.domain.lecture.repository.AssignmentSubmissionRepository;
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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LectureService {

  private final LectureRepository lectureRepository;
  private final AssignmentRepository assignmentRepository;
  private final AssignmentSubmissionRepository assignmentSubmissionRepository;
  private final GenerationQueryService generationQueryService;
  private final CategoryQueryService categoryQueryService;
  private final UserAccountService userAccountService;

  public LectureResult.ListResult getLectures(
      Long userId, Long trackId, Long subCategoryId, Pageable pageable) {
    GenerationSummary generation = requireActiveMember(userId);

    Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
    Page<Lecture> lectures =
        lectureRepository.findPublishedPage(generation.id(), trackId, subCategoryId, pageOnly);

    List<CategorySummary> tracks = categoryQueryService.findAllTracksWithSubCategories();
    Map<Long, String> subCategoryNames = new HashMap<>();
    Map<Long, String> trackNameBySubCategoryId = new HashMap<>();
    Map<Long, String> trackNames = new HashMap<>();
    for (CategorySummary track : tracks) {
      trackNames.put(track.id(), track.name());
      for (CategorySummary.SubCategoryBrief sub : track.subCategories()) {
        subCategoryNames.put(sub.id(), sub.name());
        trackNameBySubCategoryId.put(sub.id(), track.name());
      }
    }

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
          lecture.getSubCategoryId() != null ? subCategoryNames.get(lecture.getSubCategoryId()) : null,
          lecture.getSubCategoryId() != null
              ? trackNameBySubCategoryId.get(lecture.getSubCategoryId())
              : trackNames.get(lecture.getTrackId()),
          lecture.getDurationMinutes(),
          assignment != null ? KstDateTimes.toOffset(assignment.getDeadline()) : null,
          assignment != null && submittedAssignmentIds.contains(assignment.getId()));
    });

    return LectureResult.ListResult.of(buildTabs(tracks, generation.id()), content);
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
}
