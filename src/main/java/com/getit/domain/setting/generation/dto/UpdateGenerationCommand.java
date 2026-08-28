package com.getit.domain.setting.generation.dto;

/**
 * 10.2 진행 기수 · 연도 저장 명령. (PR #76 Copilot 리뷰 지적)
 *
 * <p>{@code generationNo} · {@code year} 는 둘 다 {@code Integer} 라, 서비스 메서드가 두 인자를
 * 그대로 받으면 호출부에서 순서가 바뀌어도 컴파일 에러 없이 통과한다. record 로 묶어서 인자
 * 순서 실수를 컴파일 타임에 막는다({@code UserExportFilter} 와 동일한 이유).
 */
public record UpdateGenerationCommand(
    Integer generationNo,
    Integer year
) { }
