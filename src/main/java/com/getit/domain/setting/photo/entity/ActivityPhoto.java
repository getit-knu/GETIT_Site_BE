package com.getit.domain.setting.photo.entity;

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

import com.getit.global.entity.BaseTimeEntity;

/**
 * 홈 화면에 흐르는 활동 사진 한 장.
 *
 * <p>{@code order} 는 1 부터 연속이다. 형제 항목을 밀어가며 유지한다 (FAQ 와 같은 규칙).
 */
@Entity
@Table(name = "activity_photo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityPhoto extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "photo_order", nullable = false)
  private int order;

  @Column(name = "file_id", nullable = false)
  private Long fileId;

  @Column(nullable = false)
  private boolean isVisible;

  @Builder(access = AccessLevel.PRIVATE)
  private ActivityPhoto(Long fileId, int order, boolean isVisible) {
    this.fileId = fileId;
    this.order = order;
    this.isVisible = isVisible;
  }

  public static ActivityPhoto create(Long fileId, int order, boolean isVisible) {
    return ActivityPhoto.builder().fileId(fileId).order(order).isVisible(isVisible).build();
  }

  /** 사진 교체와 노출 여부 변경. 순서는 {@link #updateOrder} 로 따로 다룬다. */
  public void update(Long fileId, boolean isVisible) {
    this.fileId = fileId;
    this.isVisible = isVisible;
  }

  public void updateOrder(int order) {
    this.order = order;
  }
}
