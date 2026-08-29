package com.getit.domain.setting.home.service;

import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;
import com.getit.domain.recruitment.service.RecruitmentScheduleWriteService;
import com.getit.domain.setting.category.service.CategoryBulkService;
import com.getit.domain.setting.category.service.TrackUpsert;
import com.getit.domain.setting.curriculum.service.CurriculumBulkService;
import com.getit.domain.setting.curriculum.service.CurriculumUpsert;
import com.getit.domain.setting.event.service.EventBulkService;
import com.getit.domain.setting.event.service.EventUpsert;
import com.getit.domain.setting.faq.service.FaqBulkService;
import com.getit.domain.setting.faq.service.FaqUpsert;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.domain.setting.home.dto.HomeSaveRequest;
import com.getit.domain.setting.home.dto.HomeSaveResult;
import com.getit.domain.setting.home.exception.HomeErrorCode;
import com.getit.global.exception.BusinessException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 홈 화면 일괄 저장. (API 명세서 10.20)
 *
 * <p>일정(recruitment) · 커리큘럼(A) · 분류 트리·행사·FAQ(B, 이슈 #133) 를 한 트랜잭션으로
 * 반영한다. 각 계약이 이미 diff(수정/생성/삭제 판단)와 order 재계산을 책임지므로, 이 서비스는
 * 요청을 각 계약의 Upsert 타입으로 옮겨 담아 호출만 한다 — 새 업무 로직을 만들지 않는다.
 *
 * <p>{@code Clock} 은 새 빈을 만들지 않고 이미 있는 빈(recruitment 소유, Asia/Seoul 고정)을
 * 재사용한다 — 대시보드(D5) 작업 때 확인한 원칙과 동일(두 번째 무한정 Clock 빈은 기존 주입
 * 지점을 모호하게 만든다).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HomeSaveService {

  private final GenerationQueryService generationQueryService;
  private final RecruitmentScheduleWriteService recruitmentScheduleWriteService;
  private final CurriculumBulkService curriculumBulkService;
  private final CategoryBulkService categoryBulkService;
  private final EventBulkService eventBulkService;
  private final FaqBulkService faqBulkService;
  private final Clock clock;

  public HomeSaveResult save(HomeSaveRequest request, boolean force) {
    GenerationSummary activeGeneration = validateActiveGeneration(request.generation());

    recruitmentScheduleWriteService.updateSchedule(activeGeneration, toScheduleCommand(request.schedule()));
    categoryBulkService.replaceTree(toTrackUpserts(request.tracks()), force);
    curriculumBulkService.replaceAll(activeGeneration.id(), toCurriculumUpserts(request.curriculums()));
    eventBulkService.replaceAll(activeGeneration.generationNo(), toEventUpserts(request.events()));
    faqBulkService.replaceAll(toFaqUpserts(request.faqs()));

    return new HomeSaveResult(OffsetDateTime.now(clock), activeGeneration.generationNo());
  }

  /**
   * 활성 기수를 딱 한 번 조회해 그 결과를 이후 모든 계약 호출에 그대로 넘긴다 — 각 계약이 스스로
   * 다시 조회하게 두면, 저장 도중 활성 기수가 바뀌었을 때(동시 admin 작업) 한 요청이 서로 다른
   * 두 기수에 걸쳐 반영될 수 있다(PR #136 Copilot 리뷰 지적). {@code generationNo}·{@code year}
   * 둘 다 요청과 일치하는지 확인한다 — {@code year}만 어긋난 요청은 지금까지 조용히 통과했다.
   */
  private GenerationSummary validateActiveGeneration(HomeSaveRequest.GenerationInfo requested) {
    GenerationSummary activeGeneration = generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(HomeErrorCode.ACTIVE_GENERATION_NOT_FOUND));
    if (!activeGeneration.generationNo().equals(requested.generationNo())
        || !activeGeneration.year().equals(requested.year())) {
      throw new BusinessException(HomeErrorCode.GENERATION_NOT_ACTIVE);
    }
    return activeGeneration;
  }

  private ScheduleUpdateCommand toScheduleCommand(HomeSaveRequest.ScheduleInfo schedule) {
    return new ScheduleUpdateCommand(
        schedule.totalStartAt(), schedule.totalEndAt(),
        schedule.documentStartAt(), schedule.documentEndAt(), schedule.interviewStartAt());
  }

  private List<TrackUpsert> toTrackUpserts(List<HomeSaveRequest.TrackInfo> tracks) {
    return tracks.stream()
        .map(track -> new TrackUpsert(track.id(), track.name(), toSubCategoryNodes(track.subCategories())))
        .toList();
  }

  private List<TrackUpsert.SubCategoryNode> toSubCategoryNodes(List<HomeSaveRequest.SubCategoryInfo> subCategories) {
    return subCategories.stream()
        .map(subCategory -> new TrackUpsert.SubCategoryNode(subCategory.id(), subCategory.name()))
        .toList();
  }

  private List<CurriculumUpsert> toCurriculumUpserts(List<HomeSaveRequest.CurriculumInfo> curriculums) {
    return curriculums.stream()
        .map(curriculum -> new CurriculumUpsert(curriculum.id(), curriculum.title(), curriculum.subtitle()))
        .toList();
  }

  private List<EventUpsert> toEventUpserts(List<HomeSaveRequest.EventInfo> events) {
    return events.stream()
        .map(event -> new EventUpsert(
            event.id(), event.title(), event.place(), event.startDate(), event.endDate(),
            event.type(), event.isVisible()))
        .toList();
  }

  private List<FaqUpsert> toFaqUpserts(List<HomeSaveRequest.FaqInfo> faqs) {
    return faqs.stream()
        .map(faq -> new FaqUpsert(faq.id(), faq.question(), faq.answer(), faq.isVisible()))
        .toList();
  }
}
