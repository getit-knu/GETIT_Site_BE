package com.getit.domain.recruitment.service;

import com.getit.domain.recruitment.dto.ApplicationPromotionSummary;
import java.util.List;

/**
 * 다른 도메인이 지원서를 조회할 때 거치는 계약. (작업 분할 계획 4.2 크로스 도메인 계약, 이슈 #66)
 *
 * <p>{@code ApplicationRepository} 직접 참조를 대체한다. 현재 소비자는 {@code user}
 * (9.4 합격자 일괄 승격) — {@code GenerationQueryService}(#22) · {@code UserQueryService}(#30) 와
 * 같은 패턴이다.
 */
public interface ApplicationQueryService {

  /** 특정 기수의 FINAL_PASS 지원서 전체. (9.4, applicationIds 미지정 시) */
  List<ApplicationPromotionSummary> findFinalPassByGenerationId(Long generationId);

  /**
   * 지정한 id 중 같은 기수 · FINAL_PASS 인 것만. (9.4, applicationIds 지정 시)
   * 요청에 있었지만 결과에 없는 id 는 호출부가 "승격 대상 아님"으로 skip 처리해야 한다.
   */
  List<ApplicationPromotionSummary> findFinalPassByIdsAndGenerationId(List<Long> applicationIds, Long generationId);
}
