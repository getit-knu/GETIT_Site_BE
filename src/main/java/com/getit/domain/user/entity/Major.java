package com.getit.domain.user.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 전공. (API 명세서 2.7)
 *
 * <p>{@code College} 와 같은 이유로 조회 전용이다 — 관리자 CRUD 가 명세서에 없다
 * (이슈 #41 논의 필요 사항 참고). {@code collegeId} 는 이 프로젝트 관례대로 JPA 연관관계 없이
 * plain 컬럼으로 둔다 ({@code generationId} 패턴과 동일).
 */
@Entity
@Table(name = "major", uniqueConstraints = {
    @UniqueConstraint(name = "uk_major_college_name", columnNames = {"college_id", "name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Major extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long collegeId;

  @Column(nullable = false, length = 50)
  private String name;

  @Builder(access = AccessLevel.PRIVATE)
  private Major(Long collegeId, String name) {
    this.collegeId = collegeId;
    this.name = name;
  }

  public static Major create(Long collegeId, String name) {
    return Major.builder().collegeId(collegeId).name(name).build();
  }
}
