package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaInvestor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FestaInvestorRepository extends JpaRepository<FestaInvestor, Long> {

  Optional<FestaInvestor> findByUserId(Long userId);

  /** 번호와 동의 기록만 비운다. 투자 내역과 집계는 남긴다. 파기한 행 수를 돌려준다. */
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("""
      update FestaInvestor v
      set v.phoneNumber = null, v.privacyConsentedAt = null
      where v.phoneNumber is not null
      """)
  int destroyAllPhoneNumbers();
}
