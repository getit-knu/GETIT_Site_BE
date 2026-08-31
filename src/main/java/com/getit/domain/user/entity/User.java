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

  /**
   * 스스로 올린 프로필 사진의 파일 id. 구글 사진을 그대로 쓰는 동안은 null.
   *
   * <p>사진을 바꿀 때 이전 파일을 연결 해제하려고 들고 있는다. 화면에 쓰는 주소는
   * {@link #profileImageUrl} 에 함께 저장한다. 프로필 사진은 공개 컨테이너라 주소가 고정이다.
   */
  @Column
  private Long profileFileId;

  /**
   * 한 번이라도 프로필을 스스로 고쳤는지. (이슈 #147)
   *
   * <p>{@link #updateProfile} 은 OAuth 재로그인마다 이름과 사진을 구글 값으로 덮어쓴다.
   * 이 표시가 없으면 자기 수정한 값이 다음 로그인에 조용히 사라진다.
   */
  @Column(nullable = false)
  private boolean profileCustomized;

  /** 소속 기수. GUEST 는 아직 소속이 없으므로 null. */
  @Column
  private Integer generationNo;

  /**
   * 소속 조. 미배정이면 null. {@code Group} 을 참조하는 FK 지만, 이 프로젝트 컨벤션대로
   * JPA 연관관계 없이 값만 갖는다. (API 명세서 9.2 · 9.10 · 9.11)
   */
  @Column
  private Long groupId;

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

  /**
   * OAuth 재로그인 시 Google 쪽에서 바뀐 값을 반영한다. email 과 providerId 는 식별자라 갱신하지 않는다.
   *
   * <p>스스로 고친 적이 있으면 건드리지 않는다. 덮어쓰면 자기 수정이 다음 로그인에
   * 조용히 사라진다 (이슈 #147).
   */
  public void updateProfile(String name, String profileImageUrl) {
    if (profileCustomized) {
      return;
    }
    this.name = name;
    this.profileImageUrl = profileImageUrl;
  }

  /**
   * 본인이 직접 고치는 프로필. (이슈 #147)
   *
   * <p>학과 · 학번 · 기수 · 권한 · 상태는 대상이 아니다. 그 값들은 지원서와 어드민 승격으로
   * 정해지는 것이라, 본인이 바꾸면 심사 결과와 어긋난다.
   *
   * @param profileImageUrl 새 사진 주소. 사진을 바꾸지 않으면 {@code null} 을 넘긴다
   * @param profileFileId 새 사진의 파일 id. 사진을 바꾸지 않으면 {@code null} 을 넘긴다
   */
  public void editProfile(String name, String phoneNumber, String profileImageUrl, Long profileFileId) {
    this.name = name;
    this.phoneNumber = phoneNumber;
    if (profileFileId != null) {
      this.profileImageUrl = profileImageUrl;
      this.profileFileId = profileFileId;
    }
    this.profileCustomized = true;
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

  /** 조 배정. (9.2 PUT /admin/users/{id}, 9.10 POST /admin/groups/{groupId}/members) */
  public void assignToGroup(Long groupId) {
    this.groupId = groupId;
  }

  /** 조 배정 해제. (9.9 조 삭제, 9.11 조원 빼기) */
  public void leaveGroup() {
    this.groupId = null;
  }

  /** 권한 변경. (9.2 PUT /admin/users/{id}) */
  public void updateRole(Role role) {
    this.role = role;
  }

  /** 소속 기수 변경. (9.2 PUT /admin/users/{id}) */
  public void updateGenerationNo(Integer generationNo) {
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
