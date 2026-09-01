package com.getit.domain.user.dto;

import com.getit.domain.user.entity.Role;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.entity.UserStatus;
import java.time.LocalDateTime;

/**
 * 다른 도메인에 노출하는 사용자 정보.
 *
 * <p>User 엔티티를 패키지 밖으로 내보내지 않기 위한 경계다.
 * 엔티티를 그대로 넘기면 소비자가 상태 변경 메서드까지 호출할 수 있고,
 * 필드가 바뀔 때마다 다른 도메인이 함께 깨진다.
 *
 * @see com.getit.domain.user.service.UserAccountService
 */
public record UserAccount(
    Long id,
    String email,
    String name,
    String phoneNumber,
    String college,
    String major,
    Integer studentYear,
    String studentNumber,
    String profileImageUrl,
    Role role,
    Integer generationNo,
    UserStatus status,

    /** 개인정보 수집·이용에 동의한 시각. 아직 동의하지 않았으면 null. (이슈 #203) */
    LocalDateTime privacyConsentedAt
) {

  /** user 패키지 내부에서만 호출한다. */
  public static UserAccount from(User user) {
    return new UserAccount(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getPhoneNumber(),
        user.getCollege(),
        user.getMajor(),
        user.getStudentYear(),
        user.getStudentNumber(),
        user.getProfileImageUrl(),
        user.getRole(),
        user.getGenerationNo(),
        user.getStatus(),
        user.getPrivacyConsentedAt()
    );
  }
}
