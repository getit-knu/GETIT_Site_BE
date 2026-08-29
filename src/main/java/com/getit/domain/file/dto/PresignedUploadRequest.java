package com.getit.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

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
    // 길이는 file_asset 컬럼 크기에 맞춘다. 검증이 없으면 INSERT 에서 터져
    // 400 이어야 할 응답이 500 으로 나간다.
    @NotBlank @Size(max = 255) String fileName,
    @NotBlank @Size(max = 100) String contentType,
    @NotNull @Positive Long size,
    @NotNull FilePurpose purpose
) { }
