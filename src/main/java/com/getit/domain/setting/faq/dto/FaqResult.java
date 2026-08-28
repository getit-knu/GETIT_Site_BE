package com.getit.domain.setting.faq.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.getit.domain.setting.faq.entity.Faq;

public record FaqResult(
    Long id,
    Integer order,
    String question,
    String answer,
    @JsonProperty("isVisible") boolean isVisible
) {

  public static FaqResult from(Faq faq) {
    return new FaqResult(
        faq.getId(),
        faq.getOrder(),
        faq.getQuestion(),
        faq.getAnswer(),
        faq.isVisible()
    );
  }
}
