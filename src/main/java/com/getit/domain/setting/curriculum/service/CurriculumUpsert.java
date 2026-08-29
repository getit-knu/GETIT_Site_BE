package com.getit.domain.setting.curriculum.service;

/**
 * 홈 일괄 저장(10.20)이 넘기는 커리큘럼 희망 상태 하나. (B 의 {@code FaqUpsert}·{@code EventUpsert}
 * 와 동일한 패턴 — {@code id==null}이면 생성, 있으면 수정, 목록에서 빠지면 삭제된다.)
 */
public record CurriculumUpsert(
    Long id,
    String title,
    String subtitle
) { }
