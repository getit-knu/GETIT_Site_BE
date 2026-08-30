package com.getit.domain.setting.category.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.getit.domain.setting.category.dto.TrackResult;
import com.getit.domain.setting.category.service.CategoryQueryService;
import com.getit.global.dto.ApiResponse;

/**
 * 부원용 대분류 목록. (이슈 #150)
 *
 * <p>트랙 구조는 {@code /api/admin/setting/tracks} 에도 있지만 그쪽은 ADMIN 전용이라
 * 부원 화면에서는 403 이 난다. 읽기 전용 목록만 부원에게 연다.
 *
 * <p>⚠️ <b>명세서에 없는 엔드포인트다.</b> API 명세서는 저장소 밖의
 * {@code DOCS/API_명세서.pdf} 라 코드와 함께 고칠 수 없다. 명세에 반영되지 않은
 * 엔드포인트가 여럿 쌓여 있어 별도로 모아 정리한다 (이슈 #155).
 */
@Tag(name = "Member 트랙", description = "부원용 대분류 목록")
@RestController
@RequestMapping("/api/member/tracks")
@RequiredArgsConstructor
public class TrackMemberController {

  private final CategoryQueryService categoryQueryService;

  @Operation(
      summary = "대분류 목록",
      description = "소분류가 없거나 발행된 강의가 없는 트랙도 포함한다. 강의 목록의 tabs 로는 보이지 않는다.")
  @GetMapping
  public ApiResponse<List<TrackResult>> getTracks() {
    return ApiResponse.success(
        categoryQueryService.findAllTracksWithSubCategories().stream()
            .map(TrackResult::from)
            .toList());
  }
}
