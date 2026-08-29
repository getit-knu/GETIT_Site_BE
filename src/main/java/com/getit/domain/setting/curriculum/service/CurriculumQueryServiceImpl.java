package com.getit.domain.setting.curriculum.service;

import com.getit.domain.setting.curriculum.repository.CurriculumRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumQueryServiceImpl implements CurriculumQueryService {

  private final CurriculumRepository curriculumRepository;

  @Override
  public List<CurriculumView> findByGenerationId(Long generationId) {
    if (generationId == null) {
      return List.of();
    }
    return curriculumRepository.findByGenerationIdOrderByOrderAscIdAsc(generationId).stream()
        .map(curriculum -> new CurriculumView(
            curriculum.getId(), curriculum.getOrder(), curriculum.getTitle(), curriculum.getSubtitle()))
        .toList();
  }
}
