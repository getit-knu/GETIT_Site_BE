package com.getit.domain.file.controller;

import com.getit.domain.auth.security.CustomUserDetails;
import com.getit.domain.file.dto.DownloadUrlResponse;
import com.getit.domain.file.dto.FileUploadResponse;
import com.getit.domain.file.dto.PresignedUploadRequest;
import com.getit.domain.file.dto.PresignedUploadResponse;
import com.getit.domain.file.entity.FilePurpose;
import com.getit.domain.file.service.FileService;
import com.getit.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "File", description = "공통 파일 업로드")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

  private final FileService fileService;

  @Operation(
      summary = "직접 업로드 주소 발급",
      description = "명세서 13.1. 발급받은 uploadUrl 로 파일을 직접 올린 뒤, fileId 를 도메인 API 에 넘겨 연결한다.")
  @PostMapping("/presigned-url")
  public ApiResponse<PresignedUploadResponse> issueUploadUrl(
      @Valid @RequestBody PresignedUploadRequest request,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(fileService.issueUploadUrl(request, principal.getUserId()));
  }

  @Operation(
      summary = "다운로드 주소 발급",
      description = "명세서 4.3. 비공개 저장소라 요청 시점마다 짧게 사는 주소를 발급한다.")
  @GetMapping("/{id}/download-url")
  public ApiResponse<DownloadUrlResponse> downloadUrl(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(
        fileService.downloadUrl(id, principal.getUserId(), principal.getRole()));
  }

  @Operation(summary = "Multipart 직접 업로드", description = "명세서 13.2")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public ApiResponse<FileUploadResponse> upload(
      @RequestPart MultipartFile file,
      @RequestParam FilePurpose purpose,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    return ApiResponse.success(fileService.upload(file, purpose, principal.getUserId()));
  }

  @Operation(summary = "파일 삭제", description = "명세서 13.3")
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserDetails principal
  ) {
    fileService.delete(id, principal.getUserId(), principal.getRole());
  }
}
