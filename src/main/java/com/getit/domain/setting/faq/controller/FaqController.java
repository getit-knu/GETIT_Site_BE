package com.getit.domain.setting.faq.controller;

import com.getit.domain.setting.faq.dto.FaqRequest;
import com.getit.domain.setting.faq.dto.FaqResult;
import com.getit.domain.setting.faq.service.FaqAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/faqs")
@RequiredArgsConstructor
public class FaqController {

  private final FaqAdminService faqAdminService;

  @Operation(summary = "FAQ 목록", description = "명세서 10.18")
  @GetMapping
  public ApiResponse<List<FaqResult>> getFaqs() {
    return ApiResponse.success(faqAdminService.getFaqs());
  }

  @Operation(summary = "FAQ 추가", description = "명세서 10.19")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<FaqResult> createFaq(@Valid @RequestBody FaqRequest request) {
    return ApiResponse.success(faqAdminService.createFaq(request));
  }

  @Operation(summary = "FAQ 수정", description = "명세서 10.19")
  @PutMapping("/{id}")
  public ApiResponse<FaqResult> updateFaq(
      @PathVariable Long id,
      @Valid @RequestBody FaqRequest request
  ) {
    return ApiResponse.success(faqAdminService.updateFaq(id, request));
  }

  @Operation(summary = "FAQ 삭제", description = "명세서 10.19")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteFaq(@PathVariable Long id) {
    faqAdminService.deleteFaq(id);
  }
}
