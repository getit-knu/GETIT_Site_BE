package com.getit.domain.festa.entity;

import com.getit.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추첨용 전화번호와 개인정보 수집·이용 동의. 투자 내역과 따로 둔다. (이슈 #222)
 *
 * <p>번호를 파기해도 행은 남고 {@code phoneNumber} 와 {@code privacyConsentedAt} 만 빈다.
 */
@Entity
@Table(name = "festa_investor")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FestaInvestor extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false, unique = true)
  private Long userId;

  @Column(name = "phone_number", length = 20)
  private String phoneNumber;

  @Column(name = "privacy_consented_at")
  private LocalDateTime privacyConsentedAt;

  public static FestaInvestor of(Long userId, String phoneNumber, LocalDateTime consentedAt) {
    FestaInvestor investor = new FestaInvestor();
    investor.userId = userId;
    investor.phoneNumber = phoneNumber;
    investor.privacyConsentedAt = consentedAt;
    return investor;
  }

  public boolean hasPhoneNumber() {
    return phoneNumber != null;
  }

  public void registerPhone(String phoneNumber, LocalDateTime consentedAt) {
    this.phoneNumber = phoneNumber;
    this.privacyConsentedAt = consentedAt;
  }
}
