package com.getit.domain.setting.faq.dto;

import com.getit.domain.setting.faq.service.FaqView;

public record FaqPublicResult(
    Long id,
    String question,
    String answer,
    int order
) {

  public static FaqPublicResult from(FaqView view) {
    return new FaqPublicResult(view.id(), view.question(), view.answer(), view.order());
  }
}
