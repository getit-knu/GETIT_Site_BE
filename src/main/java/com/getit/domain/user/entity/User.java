package com.getit.domain.user.entity;

import com.getit.global.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서비스 사용자. Google 최초 로그인 시 GUEST 로 생성되고, 최종 합격 후 운영진이 MEMBER 로 승격한다.
 * (설계 명세서 2.2)
 *
 * <p>생성은 정적 팩토리로만 한다. 상태 변경도 setter 가 아닌 의미 있는 메서드로 한다.
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends SoftDeletableEntity {

  /**
   * 학번 형식. 년도 4자리 + 고유번호 6자리.
   *
   * <p>지원서 DTO 등 값을 입력받는 쪽에서 {@code @Pattern(regexp = User.STUDENT_NUMBER_PATTERN)} 으로
   * 함께 쓴다. 각자 정규식을 만들면 화면마다 허용 형식이 달라진다.
   */
  public static final String STUDENT_NUMBER_PATTERN = "\\d{10}";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  /** Google 계정의 sub. OAuth 재로그인 시 사용자를 식별하는 키다. */
  @Column(nullable = false, unique = true, length = 100)
  private String providerId;

  @Column(nullable = false, length = 50)
  private String name;

  /** 지원서에서 수집한다. 최초 로그인 시점에는 null. */
  @Column(length = 20)
  private String phoneNumber;

  @Column(length = 50)
  private String college;

  @Column(length = 50)
  private String major;

  @Column
  private Integer studentYear;

  /**
   * 학번. 년도 4자리 + 고유번호 6자리. 지원서 기본 정보에서 수집한다.
   *
   * <p>요청 값 검증은 DTO 에서 {@link #STUDENT_NUMBER_PATTERN} 으로 한다.
   * DB 에도 CHECK 제약이 걸려 있다. char(10) 만으로는 길이도 형식도 강제되지 않기 때문이다.
   */
  @Column(columnDefinition = "CHAR(10)")
  private String studentNumber;

  @Column(length = 512)
  private String profileImageUrl;

  /** 소속 기수. GUEST 는 아직 소속이 없으므로 null. */
  @Column
  private Integer generationNo;

  /**
   * Hibernate 6 은 Java enum 을 MySQL 네이티브 ENUM 타입으로 매핑한다.
   * 그러면 값을 추가할 때마다 ALTER TABLE 이 필요하므로 varchar 로 고정한다.
   */
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private Role role;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private UserStatus status;

  @Builder(access = AccessLevel.PRIVATE)
  private User(
      String email,
      String providerId,
      String name,
      String profileImageUrl,
      Role role,
      UserStatus status
  ) {
    this.email = email;
    this.providerId = providerId;
    this.name = name;
    this.profileImageUrl = profileImageUrl;
    this.role = role;
    this.status = status;
  }

  /**
   * Google 최초 로그인 시 생성한다. 지원서를 내기 전이므로 연락처 · 학과 · 기수는 비어 있다.
   * (설계 명세서 1.1)
   */
  public static User createGuest(String providerId, String email, String name, String profileImageUrl) {
    return User.builder()
        .providerId(providerId)
        .email(email)
        .name(name)
        .profileImageUrl(profileImageUrl)
        .role(Role.GUEST)
        .status(UserStatus.ACTIVE)
        .build();
  }

  /** OAuth 재로그인 시 Google 쪽에서 바뀐 값을 반영한다. email 과 providerId 는 식별자라 갱신하지 않는다. */
  public void updateProfile(String name, String profileImageUrl) {
    this.name = name;
    this.profileImageUrl = profileImageUrl;
  }

  /**
   * 지원서에서 수집한 정보를 반영한다. 합격자 승격 시 지원서 값을 User 로 복사하는 데 쓴다.
   * (9.4 POST /admin/users/promote)
   */
  public void updateApplicantInfo(
      String phoneNumber,
      String college,
      String major,
      Integer studentYear,
      String studentNumber
  ) {
    this.phoneNumber = phoneNumber;
    this.college = college;
    this.major = major;
    this.studentYear = studentYear;
    this.studentNumber = studentNumber;
  }

  /** 최종 합격자 승격. (9.4 POST /admin/users/promote) */
  public void promoteToMember(Integer generationNo) {
    this.role = Role.MEMBER;
    this.generationNo = generationNo;
  }

  /**
   * 탈퇴 처리. (9.3 DELETE /admin/users/{id})
   * 지원서 · 과제 제출 · Q&A 이력을 보존해야 하므로 행을 지우지 않고 soft delete 한다.
   */
  public void withdraw() {
    this.status = UserStatus.WITHDRAWN;
    delete();
  }

  /** 탈퇴 복구. */
  public void activate() {
    this.status = UserStatus.ACTIVE;
    restore();
  }
}
