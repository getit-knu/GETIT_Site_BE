package com.getit.domain.setting.generation.service;

import com.getit.domain.setting.generation.dto.GenerationResult;
import com.getit.domain.setting.generation.dto.UpdateGenerationCommand;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.exception.GenerationErrorCode;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
   * <p>{@link #lockActivation()} 으로 활성화 로직 전체를 먼저 직렬화한 뒤에야 나머지 로직을
   * 수행한다 — 활성 기수가 하나도 없는 최초 상태에서는 {@code deactivateIfActive} 원자적
   * UPDATE 만으로 두 트랜잭션의 경합을 막지 못했다(둘 다 "활성 기수 없음"을 보고 그대로
   * 진행해버릴 수 있었다, PR #76 Copilot 리뷰 지적). 잠금을 먼저 잡으면 두 번째 트랜잭션은
   * 첫 번째가 커밋할 때까지 블록되고, 그 뒤에는 이미 활성화된 기수를 보게 되므로 아래
   * {@code deactivateIfActive} 조건부 UPDATE 가 정상적으로 409 로 처리한다.
   */
  @Transactional
  public GenerationResult updateGeneration(UpdateGenerationCommand command) {
    lockActivation();

    generationRepository.findByIsActiveTrue()
        .filter(active -> !active.getGenerationNo().equals(command.generationNo()))
        .ifPresent(active -> {
          int deactivated = generationRepository.deactivateIfActive(active.getId());
          if (deactivated == 0) {
            throw new BusinessException(GenerationErrorCode.ACTIVE_GENERATION_EXISTS);
          }
        });

    Generation target = generationRepository.findByGenerationNo(command.generationNo())
        .orElseGet(() -> generationRepository.save(Generation.create(command.generationNo(), command.year())));
    target.updateInfo(command.generationNo(), command.year());
    target.activate();

    return GenerationResult.from(target);
  }

  /**
   * 항상 존재하는 예약 행({@code generationNo=0})을 잠가서 활성화 로직 전체를 직렬화한다.
   * 이 행이 아직 없으면(최초 호출) 만들고, 그 사이 동시에 들어온 다른 트랜잭션은 이 조회에서
   * 블록된다 — {@code generationNo} 에 걸린 유니크 제약이 두 트랜잭션이 동시에 이 행을 만드는
   * 것도 막아준다({@code GroupService.createGroup} 과 같은 이유로
   * {@code DataIntegrityViolationException} 을 잡아 재조회한다).
   */
  private void lockActivation() {
    generationRepository.findByGenerationNoForUpdate(Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO)
        .orElseGet(this::createActivationLock);
  }

  private Generation createActivationLock() {
    int reserved = Generation.RESERVED_ACTIVATION_LOCK_GENERATION_NO;
    try {
      return generationRepository.saveAndFlush(Generation.create(reserved, reserved));
    } catch (DataIntegrityViolationException e) {
      return generationRepository.findByGenerationNoForUpdate(reserved).orElseThrow(() -> e);
    }
  }
}
