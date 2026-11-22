package com.getit.domain.festa.repository;

import com.getit.domain.festa.entity.FestaInvestor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

  /** 추첨 후보: 번호를 낸 참여자. 동의를 철회해 번호를 지웠거나 파기했으면 빠진다. (이슈 #238) */
  @Query("select v.userId from FestaInvestor v where v.phoneNumber is not null")
  List<Long> findUserIdsWithPhoneNumber();

  List<FestaInvestor> findAllByUserIdIn(Collection<Long> userIds);

  /** 남은 번호가 있는지. 자동 파기를 할 일이 없으면 감사 기록도 남기지 않는다. */
  boolean existsByPhoneNumberIsNotNull();

  /** 암호화 이전에 저장된 평문 행. 컨버터를 거치지 않고 저장값 그대로 읽는다. */
  @Query(value = """
      select id from festa_investor
      where phone_number is not null and phone_number not like 'v1:%'
      """, nativeQuery = true)
  List<Long> findPlainPhoneNumberIds();

  @Query(value = "select phone_number from festa_investor where id = :id", nativeQuery = true)
  String findRawPhoneNumber(@Param("id") Long id);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(value = "update festa_investor set phone_number = :stored where id = :id", nativeQuery = true)
  void updateRawPhoneNumber(@Param("id") Long id, @Param("stored") String stored);
}
