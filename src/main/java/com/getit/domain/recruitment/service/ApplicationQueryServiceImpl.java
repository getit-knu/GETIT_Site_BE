package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicationPromotionSummary;
import com.getit.domain.recruitment.entity.ApplicationStatus;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationQueryServiceImpl implements ApplicationQueryService {

  private final ApplicationRepository applicationRepository;

  @Override
  public List<ApplicationPromotionSummary> findFinalPassByGenerationId(Long generationId) {
    return applicationRepository.findByGenerationIdAndStatus(generationId, ApplicationStatus.FINAL_PASS).stream()
        .map(ApplicationPromotionSummary::from)
        .toList();
  }

  @Override
  public List<ApplicationPromotionSummary> findFinalPassByIdsAndGenerationId(
      List<Long> applicationIds, Long generationId
  ) {
    return applicationRepository
        .findByIdInAndGenerationIdAndStatus(applicationIds, generationId, ApplicationStatus.FINAL_PASS).stream()
        .map(ApplicationPromotionSummary::from)
        .toList();
  }

  @Override
  public long countSubmittedByGenerationId(Long generationId) {
    return applicationRepository.countByGenerationIdAndStatusNot(generationId, ApplicationStatus.DRAFT);
  }
}
