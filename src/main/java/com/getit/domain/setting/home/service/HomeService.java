package com.getit.domain.setting.home.service;

import com.getit.domain.file.service.FileInfo;
import com.getit.domain.file.service.FileQueryService;
import com.getit.domain.project.service.ProjectQueryService;
import com.getit.domain.project.service.ProjectView;
import com.getit.domain.recruitment.dto.RecruitmentStatusResult;
import com.getit.domain.recruitment.service.RecruitmentStatusQueryService;
import com.getit.domain.setting.curriculum.service.CurriculumQueryService;
import com.getit.domain.setting.faq.service.FaqQueryService;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureQueryService;
import com.getit.domain.setting.feature.service.FeatureView;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.setting.home.dto.HomeResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 통합 조회. (API 명세서 2.1)
 *
 * <p>홈 화면 1회 렌더에 필요한 데이터를 한 번에 모아 반환한다(N+1 방지). 활성 기수가 없으면
 * {@code generation}은 null, {@code recruitment}는 CLOSED, {@code curriculums}는 빈 리스트로
 * 응답한다 — 공개 API 라 관리자 API 처럼 404 로 실패하지 않는다({@code StaffPublicService}·
 * {@code RecruitmentStatusService}와 동일한 이유).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

  private final GenerationQueryService generationQueryService;
  private final RecruitmentStatusQueryService recruitmentStatusQueryService;
  private final CurriculumQueryService curriculumQueryService;
  private final ProjectQueryService projectQueryService;
  private final FaqQueryService faqQueryService;
  private final FeatureQueryService featureQueryService;
  private final FileQueryService fileQueryService;

  public HomeResult getHome() {
    Optional<GenerationSummary> activeGeneration = generationQueryService.findActive();

    return new HomeResult(
        toGenerationInfo(activeGeneration.orElse(null)),
        toRecruitmentInfo(recruitmentStatusQueryService.getStatus()),
        toCurriculums(activeGeneration.orElse(null)),
        toFeaturedProjects(),
        toFaqs(),
        toFeatures());
  }

  private HomeResult.GenerationInfo toGenerationInfo(GenerationSummary generation) {
    if (generation == null) {
      return null;
    }
    return new HomeResult.GenerationInfo(generation.generationNo(), generation.year());
  }

  private HomeResult.RecruitmentInfo toRecruitmentInfo(RecruitmentStatusResult status) {
    RecruitmentStatusResult.ScheduleWindow schedule = status.schedule();
    return new HomeResult.RecruitmentInfo(
        status.phase(),
        status.dDay(),
        status.message(),
        status.applyEnabled(),
        schedule == null ? null : schedule.totalStartAt(),
        schedule == null ? null : schedule.totalEndAt());
  }

  private List<HomeResult.CurriculumInfo> toCurriculums(GenerationSummary activeGeneration) {
    if (activeGeneration == null) {
      return List.of();
    }
    return curriculumQueryService.findByGenerationId(activeGeneration.id()).stream()
        .map(view -> new HomeResult.CurriculumInfo(view.id(), view.order(), view.title(), view.subtitle()))
        .toList();
  }

  private List<HomeResult.FeaturedProjectInfo> toFeaturedProjects() {
    List<ProjectView> featured = projectQueryService.findFeatured();
    Map<Long, String> thumbnailUrls = resolveThumbnailUrls(featured);

    return featured.stream()
        .map(view -> new HomeResult.FeaturedProjectInfo(
            view.id(), view.title(), view.description(),
            view.fileId() == null ? null : thumbnailUrls.get(view.fileId())))
        .toList();
  }

  /**
   * 후보 전체의 fileId 를 모아 한 번에 조회한다({@code StaffPublicService.findProfileImageUrls}와
   * 동일한 이유). {@code Collectors.toMap} 2-인자 버전은 키 중복 시 예외를 던지므로 병합 함수
   * {@code (a, b) -> a} 를 명시한다(같은 파일이 중복으로 잡혀도 500 대신 하나만 쓴다).
   */
  private Map<Long, String> resolveThumbnailUrls(List<ProjectView> views) {
    List<Long> fileIds = views.stream().map(ProjectView::fileId).filter(Objects::nonNull).distinct().toList();
    if (fileIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return fileQueryService.findAllByIds(fileIds).stream()
        .collect(Collectors.toMap(FileInfo::fileId, FileInfo::url, (a, b) -> a));
  }

  private List<HomeResult.FaqInfo> toFaqs() {
    return faqQueryService.findVisible().stream()
        .map(view -> new HomeResult.FaqInfo(view.id(), view.question(), view.answer()))
        .toList();
  }

  private HomeResult.FeaturesInfo toFeatures() {
    Map<FeatureKey, Boolean> enabledByKey = featureQueryService.findAll().stream()
        .collect(Collectors.toMap(FeatureView::key, FeatureView::enabled));
    return new HomeResult.FeaturesInfo(
        enabledByKey.getOrDefault(FeatureKey.STOCK_GAME, false),
        enabledByKey.getOrDefault(FeatureKey.MOCK_INVESTMENT, false));
  }
}
