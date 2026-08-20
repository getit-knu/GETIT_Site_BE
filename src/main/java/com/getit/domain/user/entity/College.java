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
 * 단과대학. (API 명세서 2.6)
 *
 * <p>명세서 전체에 이 데이터를 추가 · 수정 · 삭제하는 관리자 API가 없다. 마이그레이션의 시드
 * 데이터로만 존재하고, 이 엔티티는 조회 전용이다 (이슈 #41 논의 필요 사항 참고).
 */
@Entity
@Table(name = "college", uniqueConstraints = {
    @UniqueConstraint(name = "uk_college_name", columnNames = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class College extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Builder(access = AccessLevel.PRIVATE)
  private College(String name) {
    this.name = name;
  }

  public static College create(String name) {
    return College.builder().name(name).build();
  }
}
