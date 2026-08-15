package com.getit.domain.setting.generation.dto;

import com.getit.domain.setting.generation.entity.Generation;

/**
 * 기수 조회 결과. (작업 분할 계획 4.2 크로스 도메인 계약)
 *
 * <p>{@code Generation} 엔티티를 그대로 넘기지 않고 record 로 감싼다. 소비자가
 * {@code activate()} 같은 상태 변경 메서드를 호출할 수 없게 막기 위함이다 ({@code UserAccount} 패턴).
 */
public record GenerationSummary(
    Long id,
    Integer generationNo,
    Integer year
) {

  public static GenerationSummary from(Generation generation) {
    return new GenerationSummary(
        generation.getId(),
        generation.getGenerationNo(),
        generation.getYear()
    );
  }
}
