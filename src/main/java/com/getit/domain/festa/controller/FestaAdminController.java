package com.getit.domain.festa.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.festa.dto.FestaPhoneDestroyResult;
import com.getit.domain.festa.dto.FestaResetResult;
import com.getit.domain.festa.dto.FestaResultsResult;
import com.getit.domain.festa.dto.FestaScheduleRequest;
import com.getit.domain.festa.dto.FestaScheduleResult;
import com.getit.domain.festa.dto.FestaVoteResultsResult;
import com.getit.domain.festa.service.FestaAdminService;
import com.getit.domain.festa.service.FestaScheduleService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin.Festa", description = "모의투자 FESTA 어드민")
@RestController
@RequestMapping("/api/admin/festa")
@RequiredArgsConstructor
public class FestaAdminController {

  private final FestaAdminService festaAdminService;
  private final FestaScheduleService festaScheduleService;

  @Operation(summary = "투표 기간 조회", description = "이슈 #234. 미설정이면 startAt, endAt 이 null")
  @GetMapping("/schedule")
  public ApiResponse<FestaScheduleResult> getSchedule() {
    return ApiResponse.success(festaScheduleService.getSchedule());
  }

  @Operation(summary = "투표 기간 저장", description = "이슈 #234. startAt < endAt")
  @PutMapping("/schedule")
  public ApiResponse<FestaScheduleResult> saveSchedule(@Valid @RequestBody FestaScheduleRequest request) {
    return ApiResponse.success(festaScheduleService.saveSchedule(request));
  }

  @Operation(summary = "투자 결과 집계", description = "이슈 #222")
  @GetMapping("/results")
  public ApiResponse<FestaResultsResult> getResults() {
    return ApiResponse.success(festaAdminService.getResults());
  }

  @Operation(summary = "투표 득표 집계", description = "이슈 #232")
  @GetMapping("/vote-results")
  public ApiResponse<FestaVoteResultsResult> getVoteResults() {
    return ApiResponse.success(festaAdminService.getVoteResults());
  }

  /** 바이너리(CSV) 응답이라 {@code ApiResponse} envelope 을 쓰지 않는다. */
  @Operation(summary = "추첨용 참여자 명단 CSV", description = "이슈 #222, #232. 투자자와 투표자를 합친다")
  @GetMapping("/investors.csv")
  public ResponseEntity<byte[]> exportInvestors(@AuthenticationPrincipal CustomUserDetails principal) {
    byte[] csv = festaAdminService.exportInvestorsCsv(principal.getUserId());
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=festa-investors.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  @Operation(summary = "투자 데이터 초기화(리허설용)", description = "이슈 #229. 토글이 꺼져 있을 때만")
  @DeleteMapping("/investments")
  public ApiResponse<FestaResetResult> resetAll(@AuthenticationPrincipal CustomUserDetails principal) {
    return ApiResponse.success(festaAdminService.resetAll(principal.getUserId()));
  }

  @Operation(summary = "투자자 전화번호 일괄 파기", description = "이슈 #222")
  @DeleteMapping("/investors/phones")
  public ApiResponse<FestaPhoneDestroyResult> destroyPhoneNumbers(
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(festaAdminService.destroyPhoneNumbers(principal.getUserId()));
  }
}
