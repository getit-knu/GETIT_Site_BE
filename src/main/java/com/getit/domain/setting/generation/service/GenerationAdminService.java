package com.getit.domain.setting.generation.service;

import com.getit.domain.setting.generation.dto.GenerationResult;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.exception.GenerationErrorCode;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 진행 기수 · 연도 조회 · 저장. (API 명세서 10.1 · 10.2) */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenerationAdminService {

  private final GenerationRepository generationRepository;

  /** 10.1. */
  public GenerationResult getActiveGeneration() {
    Generation active = generationRepository.findByIsActiveTrue()
        .orElseThrow(() -> new BusinessException(GenerationErrorCode.ACTIVE_GENERATION_NOT_FOUND));
    return GenerationResult.from(active);
  }

  /**
   * 10.2. {@code generationNo} 가 이미 있는 기수면 그 행을 갱신 후 활성화하고, 없으면 새로
   * 만들어서 활성화한다. 기존 활성 기수가 있고 이번 요청과 다른 기수라면 함께 비활성화한다 —
   * 두 작업을 하나의 트랜잭션으로 묶어야 활성 기수 단일성이 지켜진다(명세서 명시).
   *
   * <p>비활성화는 {@link GenerationRepository#deactivateIfActive} 원자적 조건부 UPDATE 로
   * 하고, 반영 행 수가 0이면 그 사이 다른 요청이 먼저 처리한 것이므로 {@code
   * ACTIVE_GENERATION_EXISTS} 로 실패시킨다 — 서로 다른 기수를 동시에 활성화하려는 두 요청의
   * 경합을 막기 위함이다. 다만 활성 기수가 아예 없는 최초 상태에서 두 요청이 동시에 서로 다른
   * 기수를 활성화하려는 경우는 이 조건부 UPDATE 가 적용되지 않아 막지 못한다 — 활성 기수 단일성
   * 자체가 DB 제약으로 표현되지 않는 한계이고(설계 명세서 4.5), 최초 1회뿐인 부트스트랩 상황이라
   * 실질적 위험은 낮다고 판단해 별도 보강은 하지 않았다.
   */
  @Transactional
  public GenerationResult updateGeneration(Integer generationNo, Integer year) {
    generationRepository.findByIsActiveTrue()
        .filter(active -> !active.getGenerationNo().equals(generationNo))
        .ifPresent(active -> {
          int deactivated = generationRepository.deactivateIfActive(active.getId());
          if (deactivated == 0) {
            throw new BusinessException(GenerationErrorCode.ACTIVE_GENERATION_EXISTS);
          }
        });

    Generation target = generationRepository.findByGenerationNo(generationNo)
        .orElseGet(() -> generationRepository.save(Generation.create(generationNo, year)));
    target.updateInfo(generationNo, year);
    target.activate();

    return GenerationResult.from(target);
  }
}
