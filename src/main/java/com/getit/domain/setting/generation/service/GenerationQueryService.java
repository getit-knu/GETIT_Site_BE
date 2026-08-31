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

  /**
   * 현재 활성 기수를 읽되, <b>읽는 트랜잭션이 끝날 때까지 기수 전환을 막는다.</b>
   *
   * <p>"활성 기수인가"를 확인하고 그 결과로 쓰기를 하는 쪽이 쓴다. {@link #findActive} 로
   * 확인만 하면, 확인 직후 다른 트랜잭션이 새 기수를 활성화해도 이쪽은 그대로 커밋된다 —
   * 방금 지난 기수가 된 곳에 자료가 새로 생기거나 지워진다(PR #169 리뷰 지적).
   *
   * <p>공유 잠금이라 이 메서드를 쓰는 쓰기끼리는 서로 기다리지 않는다. 기다리는 것은
   * 기수 전환뿐이고, 그쪽은 1년에 몇 번이다.
   *
   * <p>최초 활성화 전에는 잠금 행 자체가 없어 잠글 대상이 없다. 그때는 활성 기수도 없으므로
   * 이 메서드를 쓰는 검증이 어차피 통과하지 않는다.
   */
  Optional<GenerationSummary> findActiveForWrite();

  /** id 로 기수를 조회한다. 없으면 empty. */
  Optional<GenerationSummary> findById(Long generationId);

  /**
   * 기수 번호(9, 8 ...)로 조회한다. 없으면 empty.
   *
   * <p>{@code user} 도메인이 9.2(사용자 기수 변경)에서 요청받은 {@code generationNo} 가
   * 실제로 존재하는 기수인지 확인하는 데 쓴다 (#61 PR 리뷰 지적).
   */
  Optional<GenerationSummary> findByGenerationNo(Integer generationNo);
}
