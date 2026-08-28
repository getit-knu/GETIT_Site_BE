package com.getit.domain.file.storage;

import java.util.Map;

/**
 * 클라이언트가 저장소로 파일을 직접 올릴 때 쓰는 1회용 정보.
 *
 * <p>파일 바이트가 서버를 거치지 않는다. 50MB 짜리 강의 자료가 VM 메모리와 대역폭을
 * 지나가지 않으므로, 업로드가 몰려도 애플리케이션이 영향을 받지 않는다.
 *
 * @param uploadUrl 클라이언트가 이 주소로 직접 올린다. 짧게 살고 이 파일에만 유효하다
 * @param fileUrl 업로드가 끝난 뒤 파일의 고정 주소. 읽으려면 별도 서명이 필요하다
 * @param method 업로드에 쓸 HTTP 메서드
 * @param headers 업로드 요청에 반드시 실어야 하는 헤더
 * @param expiresInSeconds {@code uploadUrl} 이 유효한 시간
 */
public record UploadTicket(
    String uploadUrl,
    String fileUrl,
    String method,
    Map<String, String> headers,
    int expiresInSeconds
) { }
