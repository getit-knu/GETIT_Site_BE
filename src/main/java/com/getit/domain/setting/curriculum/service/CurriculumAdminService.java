package com.getit.domain.setting.curriculum.service;

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

    return curriculumRepository.findByGenerationIdOrderByOrderAsc(activeGeneration.id()).stream()
        .map(CurriculumResult::from)
        .toList();
  }

  /** 10.11. */
  @Transactional
  public CurriculumResult createCurriculum(Long generationId, String title, String subtitle, Integer order) {
    GenerationSummary activeGeneration = validateActiveGeneration(generationId);

    Curriculum saved = curriculumRepository.save(
        Curriculum.create(activeGeneration.id(), order, title, subtitle));

    return CurriculumResult.from(saved);
  }

  /** 10.12. */
  @Transactional
  public CurriculumResult updateCurriculum(
      Long curriculumId, Long generationId, String title, String subtitle, Integer order
  ) {
    GenerationSummary activeGeneration = validateActiveGeneration(generationId);
    Curriculum curriculum = findCurriculum(curriculumId, activeGeneration.id());
    curriculum.update(activeGeneration.id(), order, title, subtitle);

    return CurriculumResult.from(curriculum);
  }

  /** 10.13. */
  @Transactional
  public void deleteCurriculum(Long curriculumId) {
    GenerationSummary activeGeneration = findActiveGeneration();
    Curriculum curriculum = findCurriculum(curriculumId, activeGeneration.id());

    curriculumRepository.delete(curriculum);
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
