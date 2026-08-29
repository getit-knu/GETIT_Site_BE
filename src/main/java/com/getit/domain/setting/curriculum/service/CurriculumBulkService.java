package com.getit.domain.setting.curriculum.service;

import java.util.List;

/**
 * 다른 도메인이 커리큘럼 목록을 통째 교체할 때 거치는 계약. (홈 일괄 저장 10.20 소비 목적,
 * B 의 {@code FaqBulkService}·{@code EventBulkService}와 동일한 패턴)
 */
public interface CurriculumBulkService {

  /**
   * {@code desired} 를 그대로 최종 상태로 만든다 — {@code id==null}은 생성, {@code id}가 있으면
   * 수정, 기존에 있었으나 {@code desired}에 없는 항목은 삭제한다. {@code order}는 배열 인덱스로
   * 재계산한다.
   */
  void replaceAll(Long generationId, List<CurriculumUpsert> desired);
}
