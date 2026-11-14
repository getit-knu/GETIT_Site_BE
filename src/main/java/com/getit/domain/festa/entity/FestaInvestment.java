package com.getit.domain.festa.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사용자가 프로젝트 하나에 넣은 가상 투자금. 0 원인 행은 만들지 않는다. */
@Entity
@Table(name = "festa_investment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaInvestment extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "project_id", nullable = false)
  private Long projectId;

  @Column(nullable = false)
  private long amount;

  public static FestaInvestment of(Long userId, Long projectId, long amount) {
    FestaInvestment investment = new FestaInvestment();
    investment.userId = userId;
    investment.projectId = projectId;
    investment.amount = amount;
    return investment;
  }
}
