package com.getit.domain.festa.security;

import com.getit.domain.festa.repository.FestaInvestorRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 암호화 이전에 평문으로 저장된 번호를 기동할 때 암호문으로 바꾼다.
 *
 * <p>컨버터는 평문도 읽게 해 두었지만, 그대로 두면 DB 에 평문이 남는다. 한 번 바꾸면 다음 기동부터
 * 대상이 없어 아무 일도 하지 않는다. 번호는 로그에 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlainPhoneNumberEncryptor {

  private final FestaInvestorRepository investorRepository;
  private final PhoneNumberCipher cipher;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void encryptPlainRows() {
    List<Long> ids = investorRepository.findPlainPhoneNumberIds();
    if (ids.isEmpty()) {
      return;
    }
    for (Long id : ids) {
      String plain = investorRepository.findRawPhoneNumber(id);
      if (plain != null && !PhoneNumberCipher.isEncrypted(plain)) {
        investorRepository.updateRawPhoneNumber(id, cipher.encrypt(plain));
      }
    }
    log.info("FESTA 평문 전화번호 {}건을 암호화했습니다.", ids.size());
  }
}
