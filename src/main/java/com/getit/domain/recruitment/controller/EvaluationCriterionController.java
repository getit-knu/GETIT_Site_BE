package com.getit.domain.recruitment.controller;

import com.getit.domain.recruitment.dto.EvaluationCriteriaSummary;
import com.getit.domain.recruitment.dto.EvaluationCriterionRequest;
import com.getit.domain.recruitment.dto.EvaluationCriterionResult;
import com.getit.domain.recruitment.service.EvaluationCriterionService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/admin/recruitment/criteria")
@RequiredArgsConstructor
public class EvaluationCriterionController {

  private final EvaluationCriterionService evaluationCriterionService;

  @Operation(summary = "평가 기준 목록", description = "명세서 6.8")
  @GetMapping
  public ApiResponse<EvaluationCriteriaSummary> getCriteria() {
    return ApiResponse.success(evaluationCriterionService.getCriteria());
  }

  @Operation(summary = "평가 기준 추가", description = "명세서 6.9")
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<EvaluationCriterionResult> createCriterion(
      @Valid @RequestBody EvaluationCriterionRequest request
  ) {
    return ApiResponse.success(evaluationCriterionService.createCriterion(
        request.name(), request.guideline(), request.maxScore()));
  }

  @Operation(summary = "평가 기준 수정", description = "명세서 6.10")
  @PutMapping("/{id}")
  public ApiResponse<EvaluationCriterionResult> updateCriterion(
      @PathVariable Long id,
      @Valid @RequestBody EvaluationCriterionRequest request
  ) {
    return ApiResponse.success(evaluationCriterionService.updateCriterion(
        id, request.name(), request.guideline(), request.maxScore()));
  }

  @Operation(summary = "평가 기준 삭제", description = "명세서 6.11")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCriterion(@PathVariable Long id) {
    evaluationCriterionService.deleteCriterion(id);
  }
}
