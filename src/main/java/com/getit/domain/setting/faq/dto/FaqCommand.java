package com.getit.domain.setting.faq.dto;

public record FaqCommand(
    String question,
    String answer,
    boolean isVisible
) { }
