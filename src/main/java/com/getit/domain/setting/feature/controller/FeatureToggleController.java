package com.getit.domain.setting.feature.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.setting.feature.dto.FeatureResult;
import com.getit.domain.setting.feature.dto.FeatureToggleRequest;
import com.getit.domain.setting.feature.entity.FeatureKey;
import com.getit.domain.setting.feature.service.FeatureToggleAdminService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Setting", description = "사이트 설정")
@RestController
@RequestMapping("/api/admin/setting/features")
@RequiredArgsConstructor
public class FeatureToggleController {

  private final FeatureToggleAdminService featureToggleAdminService;

  @Operation(summary = "기능 토글 목록", description = "명세서 10.23")
  @GetMapping
  public ApiResponse<List<FeatureResult>> getFeatures() {
    return ApiResponse.success(featureToggleAdminService.getFeatures());
  }

  @Operation(summary = "기능 토글", description = "명세서 10.24")
  @PutMapping("/{key}")
  public ApiResponse<FeatureResult> updateFeature(
      @PathVariable FeatureKey key,
      @Valid @RequestBody FeatureToggleRequest request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(
        featureToggleAdminService.updateFeature(key, request.enabled(), principal.getUserId()));
  }
}
