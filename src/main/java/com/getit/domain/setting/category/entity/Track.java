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
@Table(name = "track")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Track extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 50)
  private String name;

  @Column(name = "display_order", nullable = false)
  private Integer order;

  @Builder(access = AccessLevel.PRIVATE)
  private Track(String name, Integer order) {
    this.name = name;
    this.order = order;
  }

  public static Track create(String name, Integer order) {
    return Track.builder()
        .name(name)
        .order(order)
        .build();
  }

  public void update(String name, Integer order) {
    this.name = name;
    this.order = order;
  }
}
