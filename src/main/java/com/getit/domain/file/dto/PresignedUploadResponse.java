package com.getit.domain.file.dto;

import java.util.Map;

import com.getit.domain.file.storage.UploadTicket;

/**
 * 직접 업로드 주소. (명세 13.1)
 *
 * <p>클라이언트는 {@code uploadUrl} 로 파일을 올린 뒤, 도메인 API 에 {@code fileId} 를 넘겨
 * 리소스에 연결한다. 연결되지 않은 파일은 24 시간 뒤 정리 배치가 지운다.
 *
 * @param fileId 업로드 후 도메인 리소스에 연결할 때 쓰는 식별자
 * @param uploadUrl 이 주소로 직접 올린다. 이 파일에만, 짧은 시간만 유효하다
 * @param method 업로드에 쓸 HTTP 메서드
 * @param headers 반드시 함께 보내야 하는 헤더. 빠지면 저장소가 거부한다
 * @param expiresIn {@code uploadUrl} 유효 시간(초)
 */
public record PresignedUploadResponse(
    Long fileId,
    String uploadUrl,
    String method,
    Map<String, String> headers,
    int expiresIn
) {

  public static PresignedUploadResponse of(Long fileId, UploadTicket ticket) {
    return new PresignedUploadResponse(
        fileId, ticket.uploadUrl(), ticket.method(), ticket.headers(), ticket.expiresInSeconds());
  }
}
