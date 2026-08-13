package com.getit.domain.setting.generation.service;

import com.getit.domain.setting.generation.dto.GenerationSummary;
import java.util.Optional;

/**
 * 다른 도메인이 기수를 조회할 때 거치는 계약. (작업 분할 계획 4.2 크로스 도메인 계약, 이슈 #22)
 *
 * <p>{@code GenerationRepository} 직접 참조를 대체한다. 현재 소비자는 {@code recruitment}
 * (#16 리뷰에서 지적) 이고, {@code lecture} 도 주차별 강의 조회에 기수가 필요해 소비자로 예정돼 있다.
 *
 * <p>시그니처는 초안이다. lecture 쪽에서 필요한 조회 메서드가 확정되면 추가한다.
 */
public interface GenerationQueryService {

  /** 현재 활성 기수. 활성 기수가 없으면 empty. */
  Optional<GenerationSummary> findActive();
}
