package com.getit.domain.setting.staff.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 운영진 프로필. 홈 화면 · 운영자 화면에 노출되는 운영진 카드 하나를 나타낸다. (API 명세서 10.21 · 10.22)
 *
 * <p>{@code generationNo} 는 다른 setting 하위 도메인(curriculum 등)과 달리 {@code generationId}
 * FK 가 아니라 번호 그대로다 — {@code User.generationNo} 와 같은 패턴이다(명세서 요청 바디 명시).
 *
 * <p>{@code order} 는 {@code ApplicationQuestion} 과 같은 방식이다 — 생성 시 자동으로 다음 순번을
 * 부여하고, 별도 순서 변경(10.22)으로만 바꾼다. {@code section} 안에서만 순서가 유지된다.
 *
 * <p>{@code userId} · {@code fileId} 는 선택值이다. {@code userId} 가 없으면 표시 전용 프로필이고,
 * {@code fileId} 가 없으면 기본 이미지로 노출한다(프론트 처리).
 */
@Entity
@Table(name = "staff")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Staff extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Integer generationNo;

  /** order 는 SQL 예약어라 컬럼명을 분리한다. */
  @Column(name = "staff_order", nullable = false)
  private Integer order;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(nullable = false, length = 20)
  private StaffSection section;

  @Column(nullable = false, length = 50)
  private String staffRole;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(nullable = false, length = 100)
  private String department;

  /** 비어 있으면 서비스가 기본 문구("한줄 소개를 작성해주세요")로 채워서 응답한다. */
  @Column(length = 255)
  private String introduction;

  @Column
  private Long userId;

  @Column
  private Long fileId;

  @Builder(access = AccessLevel.PRIVATE)
  private Staff(
      Integer generationNo,
      Integer order,
      StaffSection section,
      String staffRole,
      String name,
      String department,
      String introduction,
      Long userId,
      Long fileId
  ) {
    this.generationNo = generationNo;
    this.order = order;
    this.section = section;
    this.staffRole = staffRole;
    this.name = name;
    this.department = department;
    this.introduction = introduction;
    this.userId = userId;
    this.fileId = fileId;
  }

  public static Staff create(
      Integer generationNo,
      Integer order,
      StaffSection section,
      String staffRole,
      String name,
      String department,
      String introduction,
      Long userId,
      Long fileId
  ) {
    return Staff.builder()
        .generationNo(generationNo)
        .order(order)
        .section(section)
        .staffRole(staffRole)
        .name(name)
        .department(department)
        .introduction(introduction)
        .userId(userId)
        .fileId(fileId)
        .build();
  }

  /** 10.21 수정. order 는 바꾸지 않는다 — section 이 바뀌면 서비스가 {@link #updateOrder} 로 따로 재배정한다. */
  public void update(
      StaffSection section, String staffRole, String name, String department,
      String introduction, Long userId, Long fileId
  ) {
    this.section = section;
    this.staffRole = staffRole;
    this.name = name;
    this.department = department;
    this.introduction = introduction;
    this.userId = userId;
    this.fileId = fileId;
  }

  /** 10.22 순서 변경, 그리고 10.21 에서 section 이 바뀌어 재배정이 필요할 때 쓴다. */
  public void updateOrder(Integer order) {
    this.order = order;
  }
}
