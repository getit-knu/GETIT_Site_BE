package com.getit.domain.qna.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.qna.dto.MemberQuestionRequest;
import com.getit.domain.qna.dto.MemberQuestionResult;
import com.getit.domain.qna.service.QuestionService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member.Qna", description = "부원 강의 Q&A")
@RestController
@RequestMapping("/api/member/lectures")
@RequiredArgsConstructor
public class QuestionController {

  private final QuestionService questionService;

  @Operation(summary = "강의 Q&A 목록", description = "명세서 4.6")
  @GetMapping("/{lectureId}/questions")
  public ApiResponse<List<MemberQuestionResult.ListItem>> getQuestions(
      @PathVariable Long lectureId,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(questionService.getMyQuestions(lectureId, principal.getUserId()));
  }

  @Operation(summary = "강의 질문 등록", description = "명세서 4.7")
  @PostMapping("/{lectureId}/questions")
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<MemberQuestionResult.CreateResult> createQuestion(
      @PathVariable Long lectureId,
      @Valid @RequestBody MemberQuestionRequest.Create request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(questionService.create(lectureId, request, principal.getUserId()));
  }
}
