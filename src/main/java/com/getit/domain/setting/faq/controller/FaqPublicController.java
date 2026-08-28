package com.getit.domain.setting.faq.controller;

import com.getit.domain.setting.faq.dto.FaqPublicResult;
import com.getit.domain.setting.faq.service.FaqQueryService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/faqs")
@RequiredArgsConstructor
public class FaqPublicController {

  private final FaqQueryService faqQueryService;

  @Operation(summary = "FAQ 목록", description = "명세서 2.5")
  @GetMapping
  public ApiResponse<List<FaqPublicResult>> getFaqs() {
    return ApiResponse.success(faqQueryService.findVisible().stream()
        .map(FaqPublicResult::from)
        .toList());
  }
}
