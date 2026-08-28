package com.getit.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.getit.domain.file.entity.FilePurpose;

/**
 * 직접 업로드 주소 발급 요청. (명세 13.1)
 *
 * @param fileName 원본 파일명. 확장자 검증에 쓴다
 * @param contentType 업로드 시 실을 Content-Type
 * @param size 파일 크기. 발급 전에 용량 제한을 확인한다
 * @param purpose 용도. 허용 확장자와 최대 용량이 여기서 정해진다
 */
public record PresignedUploadRequest(
    @NotBlank String fileName,
    @NotBlank String contentType,
    @NotNull @Positive Long size,
    @NotNull FilePurpose purpose
) { }
