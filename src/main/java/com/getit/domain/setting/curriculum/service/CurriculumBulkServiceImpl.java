package com.getit.domain.setting.curriculum.service;

import com.getit.domain.setting.curriculum.entity.Curriculum;
import com.getit.domain.setting.curriculum.exception.CurriculumErrorCode;
import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커리큘럼 목록 통째 교체. (홈 일괄 저장 10.20 소비 목적)
 *
 * <p>존재하지 않는 id 가 섞이면 예외를 던져 트랜잭션 전체가 롤백된다 — B 의
 * {@code FaqBulkServiceImpl}·{@code EventBulkServiceImpl}과 동일한 원칙.
 */
@Service
@RequiredArgsConstructor
public class CurriculumBulkServiceImpl implements CurriculumBulkService {

  private final CurriculumRepository curriculumRepository;

  @Override
  @Transactional
  public void replaceAll(Long generationId, List<CurriculumUpsert> desired) {
    Map<Long, Curriculum> existingById = curriculumRepository.findByGenerationIdForUpdate(generationId).stream()
        .collect(Collectors.toMap(Curriculum::getId, curriculum -> curriculum));

    for (int i = 0; i < desired.size(); i++) {
      CurriculumUpsert upsert = desired.get(i);
      int order = i + 1;
      if (upsert.id() == null) {
        curriculumRepository.save(Curriculum.create(generationId, order, upsert.title(), upsert.subtitle()));
        continue;
      }

      Curriculum existing = existingById.remove(upsert.id());
      if (existing == null) {
        throw new BusinessException(CurriculumErrorCode.CURRICULUM_NOT_FOUND);
      }
      existing.update(generationId, upsert.title(), upsert.subtitle());
      existing.updateOrder(order);
    }

    existingById.values().forEach(curriculumRepository::delete);
  }
}
