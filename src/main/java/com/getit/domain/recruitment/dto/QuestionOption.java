package com.getit.domain.recruitment.dto;

/** 객관식 · 체크박스 질문의 선택지. (API 명세서 6.4) */
public record QuestionOption(
    String id,
    String label
) { }
