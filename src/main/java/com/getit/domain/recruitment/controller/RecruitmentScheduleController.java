package com.getit.domain.recruitment.controller;

import com.getit.domain.recruitment.dto.ApplyToggleRequest;
import com.getit.domain.recruitment.dto.RecruitmentScheduleResult;
import com.getit.domain.recruitment.dto.RecruitmentScheduleUpdateRequest;
import com.getit.domain.recruitment.dto.ScheduleUpdateCommand;
import com.getit.domain.recruitment.service.RecruitmentScheduleService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Recruitment", description = "모집")
@RestController
@RequestMapping("/api/admin/recruitment/schedule")
@RequiredArgsConstructor
public class RecruitmentScheduleController {

  private final RecruitmentScheduleService recruitmentScheduleService;

  @Operation(summary = "모집 일정 조회", description = "명세서 6.1")
  @GetMapping
  public ApiResponse<RecruitmentScheduleResult> getSchedule() {
    return ApiResponse.success(recruitmentScheduleService.getSchedule());
  }

  @Operation(summary = "모집 일정 저장", description = "명세서 6.2")
  @PutMapping
  public ApiResponse<RecruitmentScheduleResult> updateSchedule(
      @Valid @RequestBody RecruitmentScheduleUpdateRequest request
  ) {
    return ApiResponse.success(recruitmentScheduleService.updateSchedule(new ScheduleUpdateCommand(
        request.totalStartAt(),
        request.totalEndAt(),
        request.documentStartAt(),
        request.documentEndAt(),
        request.interviewStartAt()
    )));
  }

  /**
   * 지원 접수를 여닫는다. (이슈 #170)
   *
   * <p>일정과 별개다. 서류 기간 중이라도 내리면 지원이 막히고, 일정 값은 그대로 남는다.
   * 급히 멈추려고 마감일을 과거로 당기면 원래 일정이 지워지고 공개 화면의 D-day 까지
   * 함께 망가진다.
   */
  @Operation(summary = "지원 접수 열기 · 닫기", description = "이슈 #170")
  @PutMapping("/apply-enabled")
  public ApiResponse<RecruitmentScheduleResult> changeApplyEnabled(
      @Valid @RequestBody ApplyToggleRequest request
  ) {
    return ApiResponse.success(recruitmentScheduleService.changeApplyEnabled(request.enabled()));
  }
}
