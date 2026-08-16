package com.getit.domain.recruitment.controller;

import com.getit.domain.recruitment.dto.ApplicationQuestionRequest;
import com.getit.domain.recruitment.dto.ApplicationQuestionResult;
import com.getit.domain.recruitment.dto.QuestionOrderRequest;
import com.getit.domain.recruitment.service.ApplicationQuestionService;
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

@Tag(name = "Recruitment", description = "모집")
@RestController
@RequestMapping("/api/admin/recruitment/questions")
@RequiredArgsConstructor
public class ApplicationQuestionController {

  private final ApplicationQuestionService applicationQuestionService;

  @Operation(summary = "질문 항목 목록", description = "명세서 6.3")
  @GetMapping
  public ApiResponse<List<ApplicationQuestionResult>> getQuestions() {
    return ApiResponse.success(applicationQuestionService.getQuestions());
  }

  @Operation(summary = "질문 추가", description = "명세서 6.4")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<ApplicationQuestionResult> createQuestion(
      @Valid @RequestBody ApplicationQuestionRequest request
  ) {
    return ApiResponse.success(applicationQuestionService.createQuestion(
        request.type(), request.content(), request.requiredOrDefault(), request.maxLength(), request.options()));
  }

  @Operation(summary = "질문 수정", description = "명세서 6.5")
  @PutMapping("/{id}")
  public ApiResponse<ApplicationQuestionResult> updateQuestion(
      @PathVariable Long id,
      @Valid @RequestBody ApplicationQuestionRequest request
  ) {
    return ApiResponse.success(applicationQuestionService.updateQuestion(
        id, request.type(), request.content(), request.requiredOrDefault(), request.maxLength(), request.options()));
  }

  @Operation(summary = "질문 삭제", description = "명세서 6.6")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteQuestion(@PathVariable Long id) {
    applicationQuestionService.deleteQuestion(id);
  }

  @Operation(summary = "질문 순서 변경", description = "명세서 6.7")
  @PutMapping("/order")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reorderQuestions(@Valid @RequestBody QuestionOrderRequest request) {
    applicationQuestionService.reorderQuestions(request.orderedIds());
  }
}
