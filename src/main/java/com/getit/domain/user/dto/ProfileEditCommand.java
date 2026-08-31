package com.getit.domain.user.dto;

/**
 * 본인이 직접 고치는 프로필 값. (이슈 #147)
 *
 * @param profileFileId 새 프로필 사진의 파일 id. {@code null} 이면 사진은 그대로 둔다
 */
public record ProfileEditCommand(String name, String phoneNumber, Long profileFileId) {
}
