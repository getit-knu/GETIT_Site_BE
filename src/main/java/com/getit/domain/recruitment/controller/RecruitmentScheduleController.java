package com.getit.domain.recruitment.controller;

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
}
