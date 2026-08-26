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
 * 조. 기수별로 부원을 나눠 관리하는 단위다. (API 명세서 9.6 ~ 9.11)
 *
 * <p>테이블명은 {@code group}이 SQL 예약어라 {@code user_group}으로 잡는다.
 *
 * <p>조원 소속은 이 엔티티가 아니라 {@link User#getGroupId()} (FK 1:N)로 표현한다. 한 사용자가
 * 여러 조에 속할 수 없다는 정책(9.10 {@code ALREADY_IN_GROUP}) 때문에 별도 매핑 테이블 없이
 * 단순 FK로 충분하다.
 */
@Entity
@Table(name = "user_group", uniqueConstraints = {
    @UniqueConstraint(name = "uk_user_group_generation_name", columnNames = {"generation_id", "name"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private Long generationId;

  @Column(nullable = false, length = 50)
  private String name;

  @Builder(access = AccessLevel.PRIVATE)
  private Group(Long generationId, String name) {
    this.generationId = generationId;
    this.name = name;
  }

  public static Group create(Long generationId, String name) {
    return Group.builder()
        .generationId(generationId)
        .name(name)
        .build();
  }

  /** 조 이름 수정. (9.8 PUT /admin/groups/{id}) */
  public void rename(String name) {
    this.name = name;
  }
}
