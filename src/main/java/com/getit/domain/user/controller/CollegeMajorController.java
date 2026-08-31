package com.getit.domain.user.controller;

import com.getit.domain.user.dto.CollegeResult;
import com.getit.domain.user.dto.MajorResult;
import com.getit.domain.user.service.CollegeMajorService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class CollegeMajorController {

  private final CollegeMajorService collegeMajorService;

  @Operation(summary = "단과대학 목록", description = "명세서 2.6")
  @GetMapping("/colleges")
  public ApiResponse<List<CollegeResult>> getColleges() {
    return ApiResponse.success(collegeMajorService.getColleges());
  }

  /**
   * 명세서 2.7 의 경로. 지원서 폼의 단과대 · 학과 셀렉트가 쓴다. (이슈 #193)
   *
   * <p>없는 단과대 id 면 빈 목록이다. 404 로 나누지 않는다 — 화면 입장에서 "그런 단과대가
   * 없다" 와 "그 단과대에 학과가 없다" 는 똑같이 고를 것이 없는 상태다.
   */
  @Operation(summary = "단과대학별 전공 목록", description = "명세서 2.7")
  @GetMapping("/colleges/{collegeId}/majors")
  public ApiResponse<List<MajorResult>> getMajorsOfCollege(@PathVariable Long collegeId) {
    return ApiResponse.success(collegeMajorService.getMajors(collegeId));
  }

  /**
   * 전체 학과. 중첩 경로로는 표현할 수 없어 남겨 둔다.
   *
   * <p>{@code collegeId} 를 주면 위와 같은 결과다. 같은 서비스 메서드를 쓰므로 동작이
   * 갈라지지 않는다.
   */
  @Operation(summary = "전공 목록", description = "명세서 2.7 (전체 조회)")
  @GetMapping("/majors")
  public ApiResponse<List<MajorResult>> getMajors(
      @RequestParam(required = false) Long collegeId
  ) {
    return ApiResponse.success(collegeMajorService.getMajors(collegeId));
  }
}
