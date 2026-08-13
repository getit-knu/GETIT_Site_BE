package com.getit.domain.setting.generation.service;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import com.getit.domain.setting.generation.repository.GenerationRepository;
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
}
