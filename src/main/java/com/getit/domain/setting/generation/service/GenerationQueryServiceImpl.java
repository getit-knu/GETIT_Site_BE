package com.getit.domain.setting.generation.service;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenerationQueryServiceImpl implements GenerationQueryService {

  private final GenerationRepository generationRepository;

  @Override
  public Optional<GenerationSummary> findActive() {
    return generationRepository.findByIsActiveTrue().map(GenerationSummary::from);
  }

  @Override
  public Optional<GenerationSummary> findById(Long generationId) {
    return generationRepository.findById(generationId).map(GenerationSummary::from);
  }

  /**
   * {@code generationNo} 가 활성화 로직 잠금용 예약 값이면 무조건 빈 값을 반환한다 —
   * {@code GenerationAdminService} 내부 구현 세부사항일 뿐, 다른 도메인이 실제 기수로
   * 오인해서는 안 된다({@link Generation#RESERVED_ACTIVATION_LOCK_GENERATION_NO}).
   */
  @Override
  public Optional<GenerationSummary> findByGenerationNo(Integer generationNo) {
    if (Objects.equals(generationNo, Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO)) {
      return Optional.empty();
    }
    return generationRepository.findByGenerationNo(generationNo).map(GenerationSummary::from);
  }
}
