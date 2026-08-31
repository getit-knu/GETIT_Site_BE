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

  /**
   * 활성화 잠금 행을 공유 모드로 먼저 잡아, 이 트랜잭션이 끝날 때까지 기수 전환을 막는다.
   *
   * <p>{@code GenerationAdminService.lockActivation} 이 같은 행을 배타 모드로 잡는다.
   * 그래서 전환은 여기서 잡은 공유 잠금이 풀릴 때까지 기다린다.
   */
  @Override
  public Optional<GenerationSummary> findActiveForWrite() {
    generationRepository.findByGenerationNoShared(
        Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO);
    return findActive();
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
