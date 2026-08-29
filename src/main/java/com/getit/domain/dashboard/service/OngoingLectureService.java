package com.getit.domain.dashboard.service;

import com.getit.domain.dashboard.dto.OngoingLectureResult;
import com.getit.domain.lecture.service.LectureStatService;
import com.getit.domain.lecture.service.OngoingLectureStat;
import com.getit.domain.setting.category.dto.CategorySummary;
import com.getit.domain.setting.category.service.CategoryQueryService;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.user.service.UserQueryService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 진행 중 강의. (API 명세서 5.5) 활성 기수가 없으면 빈 리스트로 응답한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OngoingLectureService {

  private final GenerationQueryService generationQueryService;
  private final UserQueryService userQueryService;
  private final LectureStatService lectureStatService;
  private final CategoryQueryService categoryQueryService;

  public List<OngoingLectureResult> getOngoingLectures() {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();
    if (activeGeneration.isEmpty()) {
      return List.of();
    }

    GenerationSummary generation = activeGeneration.get();
    List<OngoingLectureStat> lectures = lectureStatService.findOngoingLectures(generation.generationNo());
    if (lectures.isEmpty()) {
      return List.of();
    }

    long totalCount = userQueryService.countActiveMembersInGeneration(generation.generationNo());
    Map<Long, String> subCategoryNames = resolveSubCategoryNames();

    return lectures.stream().map(lecture -> toResult(lecture, subCategoryNames, totalCount)).toList();
  }

  /**
   * {@code CategoryQueryService}에 subCategory 이름 단건 조회가 없어, 트랙 전체를 한 번 조회한 뒤
   * subCategory 를 평탄화해서 id 로 찾는다 — 진행 중 강의 수가 많아도 카테고리 조회는 한 번뿐이다.
   */
  private Map<Long, String> resolveSubCategoryNames() {
    return categoryQueryService.findAllTracksWithSubCategories().stream()
        .flatMap(track -> track.subCategories().stream())
        .collect(Collectors.toMap(CategorySummary.SubCategoryBrief::id, CategorySummary.SubCategoryBrief::name));
  }

  private OngoingLectureResult toResult(
      OngoingLectureStat lecture, Map<Long, String> subCategoryNames, long totalCount
  ) {
    return new OngoingLectureResult(
        lecture.lectureId(),
        lecture.title(),
        lecture.subCategoryId() == null ? null : subCategoryNames.get(lecture.subCategoryId()),
        lecture.deadline().toLocalDate(),
        lecture.submittedCount(),
        totalCount);
  }
}
