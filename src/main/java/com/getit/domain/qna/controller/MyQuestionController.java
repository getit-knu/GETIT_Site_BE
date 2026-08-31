package com.getit.domain.qna.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.qna.dto.MemberQuestionResult;
import com.getit.domain.qna.entity.QnaStatus;
import com.getit.domain.qna.service.QuestionService;
import com.getit.global.dto.ApiResponse;
import com.getit.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부원이 자기가 쓴 질문을 강의와 무관하게 한 번에 본다. (이슈 #185)
 *
 * <p>강의별 조회(4.6)는 {@code /api/member/lectures/{lectureId}/questions} 라 강의 id 를 알아야
 * 한다. 마이페이지의 "내 질문" 은 강의를 가로질러 봐야 하는데, 그러려면 프론트가 전체 강의를
 * 순회해 N 번 호출하고 페이징 · 정렬도 직접 맞춰야 했다.
 */
@Tag(name = "Member.Qna", description = "부원 강의 Q&A")
@RestController
@RequestMapping("/api/member/questions")
@RequiredArgsConstructor
public class MyQuestionController {

  private final QuestionService questionService;

  /**
   * @param status 비우면 전체. 답변 대기 · 답변 완료로 거를 때 쓴다
   */
  @Operation(summary = "내 질문 전체 조회", description = "이슈 #185")
  @GetMapping
  public ApiResponse<PageResponse<MemberQuestionResult.MyListItem>> getMyQuestions(
      @AuthenticationPrincipal CustomUserDetails principal,
      @RequestParam(required = false) QnaStatus status,
      @PageableDefault(size = 20) Pageable pageable
  ) {
    return ApiResponse.success(
        questionService.getMyQuestions(principal.getUserId(), status, pageable));
  }
}
