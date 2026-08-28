package com.getit.domain.setting.feature.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 기능 활성화 토글 한 건. (명세서 10.23 · 10.24) */
@Entity
@Table(name = "feature_toggle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeatureToggle extends BaseTimeEntity {

  @Id
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.VARCHAR)
  @Column(name = "toggle_key", length = 30)
  private FeatureKey toggleKey;

  @Column(nullable = false)
  private boolean enabled;

  @Column(nullable = true)
  private Long updatedBy;

  @Builder(access = AccessLevel.PRIVATE)
  private FeatureToggle(FeatureKey toggleKey, boolean enabled) {
    this.toggleKey = toggleKey;
    this.enabled = enabled;
  }

  public static FeatureToggle create(FeatureKey toggleKey, boolean enabled) {
    return FeatureToggle.builder().toggleKey(toggleKey).enabled(enabled).build();
  }

  public void updateEnabled(boolean enabled, Long updatedBy) {
    this.enabled = enabled;
    this.updatedBy = updatedBy;
  }
}
