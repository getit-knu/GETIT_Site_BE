package com.getit.domain.user.controller;

import com.getit.domain.user.dto.CollegeResult;
import com.getit.domain.user.dto.MajorResult;
import com.getit.domain.user.service.CollegeService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Public", description = "공개 사이트")
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class CollegeController {

  private final CollegeService collegeService;

  @Operation(summary = "단과대학 목록", description = "명세서 2.6")
  @GetMapping("/colleges")
  public ApiResponse<List<CollegeResult>> getColleges() {
    return ApiResponse.success(collegeService.getColleges());
  }

  @Operation(summary = "전공 목록", description = "명세서 2.7")
  @GetMapping("/majors")
  public ApiResponse<List<MajorResult>> getMajors(
      @RequestParam(required = false) Long collegeId
  ) {
    return ApiResponse.success(collegeService.getMajors(collegeId));
  }
}
