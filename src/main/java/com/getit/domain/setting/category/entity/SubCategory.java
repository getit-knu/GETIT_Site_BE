package com.getit.domain.setting.category.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sub_category")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubCategory extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "display_order", nullable = false)
  private Integer order;

  @Column(nullable = false)
  private Long trackId;

  @Builder(access = AccessLevel.PRIVATE)
  private SubCategory(String name, Integer order, Long trackId) {
    this.name = name;
    this.order = order;
    this.trackId = trackId;
  }

  public static SubCategory create(String name, Integer order, Long trackId) {
    return SubCategory.builder()
        .name(name)
        .order(order)
        .trackId(trackId)
        .build();
  }

  public void update(String name, Integer order) {
    this.name = name;
    this.order = order;
  }
}
