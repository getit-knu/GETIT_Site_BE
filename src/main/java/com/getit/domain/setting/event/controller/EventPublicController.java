package com.getit.domain.setting.event.controller;

import com.getit.domain.setting.event.dto.EventCalendarResult;
import com.getit.domain.setting.event.service.EventPublicService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public/events")
@Validated
@RequiredArgsConstructor
public class EventPublicController {

  private final EventPublicService eventPublicService;

  @Operation(summary = "월별 행사 일정", description = "명세서 2.2")
  @GetMapping
  public ApiResponse<EventCalendarResult> getMonthlyEvents(
      @RequestParam @Min(2000) @Max(2100) int year,
      @RequestParam @Min(1) @Max(12) int month
  ) {
    return ApiResponse.success(eventPublicService.getMonthly(year, month));
  }
}
