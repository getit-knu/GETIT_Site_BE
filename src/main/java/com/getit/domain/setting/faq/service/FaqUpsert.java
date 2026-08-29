package com.getit.domain.setting.faq.service;

public record FaqUpsert(
    Long id,
    String question,
    String answer,
    boolean isVisible
) { }
