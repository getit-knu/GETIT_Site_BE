package com.getit.domain.qna.admin.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.qna.admin.dto.AdminAnswerRequest;
import com.getit.domain.qna.admin.dto.AdminAnswerResult;
import com.getit.domain.qna.admin.dto.AdminQuestionResult;
import com.getit.domain.qna.admin.service.QuestionAdminService;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.global.dto.ApiResponse;
import com.getit.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin.Qna", description = "Q&A 관리")
@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
public class QuestionAdminController {

  private static final String SITE_ONLY = "none";

  private final QuestionAdminService questionAdminService;

  @Operation(summary = "Q&A 목록", description = "명세서 11.1")
  @GetMapping
  public ApiResponse<PageResponse<AdminQuestionResult.ListRow>> getQuestions(
      @RequestParam(required = false) QnaStatus status,
      @RequestParam(required = false) String lectureId,
      @RequestParam(required = false) String keyword,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    boolean siteOnly = SITE_ONLY.equals(lectureId);
    Long lectureIdFilter = siteOnly || lectureId == null ? null : Long.valueOf(lectureId);
    return ApiResponse.success(
        questionAdminService.search(status, siteOnly, lectureIdFilter, keyword, pageable));
  }

  @Operation(summary = "질문 상세", description = "명세서 11.2")
  @GetMapping("/{id}")
  public ApiResponse<AdminQuestionResult.Detail> getQuestion(@PathVariable Long id) {
    return ApiResponse.success(questionAdminService.getDetail(id));
  }

  @Operation(summary = "답변 작성", description = "명세서 11.3")
  @PostMapping("/{id}/answer")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<AdminAnswerResult.CreateResult> createAnswer(
      @PathVariable Long id,
      @Valid @RequestBody AdminAnswerRequest.Write request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(questionAdminService.createAnswer(id, request, principal.getUserId()));
  }

  @Operation(summary = "답변 수정", description = "명세서 11.4")
  @PutMapping("/{id}/answer")
  public ApiResponse<AdminAnswerResult.UpdateResult> updateAnswer(
      @PathVariable Long id,
      @Valid @RequestBody AdminAnswerRequest.Write request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(questionAdminService.updateAnswer(id, request, principal.getUserId()));
  }
}
