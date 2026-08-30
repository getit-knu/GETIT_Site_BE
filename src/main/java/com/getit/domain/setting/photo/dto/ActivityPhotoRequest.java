package com.getit.domain.setting.photo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 활동 사진 등록 · 수정 요청.
 *
 * @param fileId 미리 업로드한 파일. {@code FilePurpose.ACTIVITY_PHOTO} 로 올린 것이어야 한다
 * @param order 표시 순서. 비우면 등록은 맨 뒤, 수정은 순서를 그대로 둔다 (FAQ 와 같은 규칙)
 */
public record ActivityPhotoRequest(
    @NotNull Long fileId,
    @NotNull Boolean isVisible,
    @Min(1) Integer order
) { }
