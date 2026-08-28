package com.getit.domain.setting.faq.dto;

import com.getit.domain.setting.faq.entity.Faq;

/** FAQ 조회 · 저장 결과. (API 명세서 10.18 ~ 10.19) */
public record FaqResult(
    Long id,
    Integer order,
    String question,
    String answer,
    boolean isVisible
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
