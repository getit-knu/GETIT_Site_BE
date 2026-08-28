package com.getit.domain.setting.curriculum.service;

import com.getit.domain.setting.curriculum.dto.CurriculumRequest;
import com.getit.domain.setting.curriculum.dto.CurriculumResult;
import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.exception.CurriculumErrorCode;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.service.GenerationQueryService;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커리큘럼 조회 · 저장. (API 명세서 10.10 ~ 10.13)
 *
 * <p>전부 활성 기수로 스코프한다 — 홈 화면에 노출되는 콘텐츠라 항상 "지금 진행 중인 기수"
 * 기준이어야 하기 때문이다({@code ApplicationQuestionService}와 동일한 이유). {@code Group}
 * (9.7)처럼 임의 기수 생성을 허용하는 설계와는 다르다 — 조는 과거 기수 조회 이력이 남아야 하지만,
 * 커리큘럼은 공개 홈 화면 콘텐츠라 그럴 필요가 없다고 판단했다(PR 리뷰 포인트).
 *
 * <p>{@code order} 는 클라이언트가 직접 값을 보내지만(별도 순서 변경 엔드포인트 없음), 1..N
 * 연속 불변식은 이 서비스가 책임진다 — 그러지 않으면 카드 노출 순서가 비결정적이 된다(PR #78
 * Copilot 리뷰 지적). 요청값을 유효 범위로 clamp 한 뒤, 그 사이에 끼어드는 다른 항목들의 순서를
 * 밀거나 당긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumAdminService {

  private final CurriculumRepository curriculumRepository;
  private final GenerationQueryService generationQueryService;

  /** 10.10. */
  public List<CurriculumResult> getCurriculums() {
    GenerationSummary activeGeneration = findActiveGeneration();

    return curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(activeGeneration.id()).stream()
        .map(CurriculumResult::from)
        .toList();
  }

  /** 10.11. 요청 order 를 [1, 기존 개수+1] 로 clamp 하고, 그 이후 항목들을 한 칸씩 뒤로 민다. */
  @Transactional
  public CurriculumResult createCurriculum(CurriculumRequest request) {
    GenerationSummary activeGeneration = validateActiveGeneration(request.generationId());
    List<Curriculum> siblings = curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(activeGeneration.id());

    int newOrder = clamp(request.order(), 1, siblings.size() + 1);
    siblings.stream()
        .filter(sibling -> sibling.getOrder() >= newOrder)
        .forEach(sibling -> sibling.updateOrder(sibling.getOrder() + 1));

    Curriculum saved = curriculumRepository.save(
        Curriculum.create(activeGeneration.id(), newOrder, request.title(), request.subtitle()));

    return CurriculumResult.from(saved);
  }

  /**
   * 10.12. 요청 order 를 [1, 전체 개수] 로 clamp 한 뒤, 현재 순서와 다르면 그 구간에 있는 다른
   * 항목들의 순서를 밀거나 당겨서 자리를 만든다.
   */
  @Transactional
  public CurriculumResult updateCurriculum(Long curriculumId, CurriculumRequest request) {
    GenerationSummary activeGeneration = validateActiveGeneration(request.generationId());
    Curriculum target = findCurriculum(curriculumId, activeGeneration.id());
    List<Curriculum> siblings = curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(activeGeneration.id());

    int currentOrder = target.getOrder();
    int newOrder = clamp(request.order(), 1, siblings.size());
    if (newOrder < currentOrder) {
      siblings.stream()
          .filter(sibling -> !sibling.getId().equals(target.getId()))
          .filter(sibling -> sibling.getOrder() >= newOrder && sibling.getOrder() < currentOrder)
          .forEach(sibling -> sibling.updateOrder(sibling.getOrder() + 1));
    } else if (newOrder > currentOrder) {
      siblings.stream()
          .filter(sibling -> !sibling.getId().equals(target.getId()))
          .filter(sibling -> sibling.getOrder() > currentOrder && sibling.getOrder() <= newOrder)
          .forEach(sibling -> sibling.updateOrder(sibling.getOrder() - 1));
    }

    target.updateOrder(newOrder);
    target.update(activeGeneration.id(), request.title(), request.subtitle());

    return CurriculumResult.from(target);
  }

  /** 10.13. 삭제된 순번 뒤 항목들을 한 칸씩 당겨서 order 결번을 막는다({@code ApplicationQuestionService}와 동일 이유). */
  @Transactional
  public void deleteCurriculum(Long curriculumId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    Curriculum curriculum = findCurriculum(curriculumId, activeGeneration.id());
    int deletedOrder = curriculum.getOrder();

    curriculumRepository.delete(curriculum);

    curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(activeGeneration.id()).stream()
        .filter(sibling -> sibling.getOrder() > deletedOrder)
        .forEach(sibling -> sibling.updateOrder(sibling.getOrder() - 1));
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(value, max));
  }

  private Curriculum findCurriculum(Long curriculumId, Long activeGenerationId) {
    return curriculumRepository.findByIdAndGenerationId(curriculumId, activeGenerationId)
        .orElseThrow(() -> new BusinessException(CurriculumErrorCode.CURRICULUM_NOT_FOUND));
  }

  private GenerationSummary findActiveGeneration() {
    return generationQueryService.findActive()
        .orElseThrow(() -> new BusinessException(CurriculumErrorCode.ACTIVE_GENERATION_NOT_FOUND));
  }

  /** 요청받은 generationId 가 활성 기수와 일치하는지 확인한다 ({@code UserPromotionService}와 동일 패턴). */
  private GenerationSummary validateActiveGeneration(Long generationId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    if (!activeGeneration.id().equals(generationId)) {
      throw new BusinessException(CurriculumErrorCode.GENERATION_NOT_FOUND);
    }
    return activeGeneration;
  }
}
