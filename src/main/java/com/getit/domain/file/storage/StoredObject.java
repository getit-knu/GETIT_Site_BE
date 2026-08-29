package com.getit.domain.file.storage;

/**
 * 저장소에 실제로 올라와 있는 파일의 정보.
 *
 * <p>업로드 주소는 클라이언트가 신고한 크기·형식으로 발급된다. SAS 자체에는 크기 제한을
 * 걸 수 없어서, 작게 신고하고 크게 올리거나 아예 올리지 않을 수 있다.
 * 연결 전에 이 값으로 실물을 확인한다.
 *
 * @param size 실제 바이트 수
 * @param contentType 저장소가 기록한 형식
 */
public record StoredObject(long size, String contentType) { }
