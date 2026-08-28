package com.getit.domain.setting.faq.service;

public record FaqView(
    Long id,
    String question,
    String answer,
    int order
) { }
