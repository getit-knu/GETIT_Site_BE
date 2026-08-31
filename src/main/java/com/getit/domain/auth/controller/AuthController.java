package com.getit.domain.auth.controller;

import com.getit.domain.auth.dto.MeResponse;
import com.getit.domain.auth.dto.MeUpdateRequest;
import com.getit.domain.auth.dto.TokenResponse;
import com.getit.domain.auth.exception.AuthErrorCode;
import com.getit.domain.auth.oauth2.RefreshTokenCookie;
import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.auth.service.AuthService;
import com.getit.domain.auth.service.RefreshTokenService;
import com.getit.domain.auth.service.RefreshTokenService.TokenPair;
import com.getit.domain.auth.jwt.JwtProvider;
import com.getit.global.dto.ApiResponse;
import com.getit.global.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final RefreshTokenCookie refreshTokenCookie;
  private final JwtProvider jwtProvider;

  @Operation(summary = "내 프로필 조회", description = "명세서 1.5")
  @GetMapping("/me")
  public ApiResponse<MeResponse> getMe(@AuthenticationPrincipal CustomUserDetails principal) {
    return ApiResponse.success(authService.getMe(principal.getUserId()));
  }

  @Operation(summary = "내 프로필 수정", description = "이슈 #147")
  @PutMapping("/me")
  public ApiResponse<MeResponse> updateMe(
      @AuthenticationPrincipal CustomUserDetails principal,
      @Valid @RequestBody MeUpdateRequest request
  ) {
    return ApiResponse.success(authService.updateMe(principal.getUserId(), request));
  }

  /**
   * Access Token 재발급. (명세서 1.3)
   *
   * <p>Refresh Token 은 HttpOnly 쿠키로만 오간다. 자바스크립트가 읽을 수 없으므로
   * 명세서와 달리 요청 본문으로 받지 않고, 응답 본문에도 담지 않는다.
   */
  @Operation(summary = "Access Token 재발급", description = "명세서 1.3")
  @PostMapping("/refresh")
  public ApiResponse<TokenResponse> refresh(
      @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
      HttpServletResponse response
  ) {
    if (!StringUtils.hasText(refreshToken)) {
      throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    TokenPair tokens = refreshTokenService.rotate(refreshToken);
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        refreshTokenCookie.create(tokens.refreshToken(), jwtProvider.getRefreshTokenValidity()).toString()
    );

    return ApiResponse.success(
        new TokenResponse(tokens.accessToken(), tokens.accessTokenExpiresIn())
    );
  }

  /**
   * 로그아웃. (명세서 1.4)
   *
   * <p>인증을 요구하지 않는다. 자기가 이미 가진 Refresh 쿠키를 폐기하는 것뿐이고,
   * 요구하면 Access Token 이 만료된 사용자가 로그아웃을 못 해 Refresh 가 최대 2주 살아남는다.
   *
   * <p>쿠키가 없어도 성공으로 처리한다. 이미 로그아웃된 상태와 구분할 필요가 없다.
   */
  @Operation(summary = "로그아웃", description = "명세서 1.4")
  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(
      @CookieValue(name = RefreshTokenCookie.NAME, required = false) String refreshToken,
      HttpServletResponse response
  ) {
    if (StringUtils.hasText(refreshToken)) {
      refreshTokenService.revoke(refreshToken);
    }
    response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.expired().toString());
  }
}
