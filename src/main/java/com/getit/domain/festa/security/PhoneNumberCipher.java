package com.getit.domain.festa.security;

import com.getit.domain.festa.config.FestaProperties;
import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * 추첨용 전화번호를 AES-256-GCM 으로 암호화한다.
 *
 * <p>DB 덤프나 백업, 읽기 권한이 새어도 키 없이는 번호를 읽을 수 없게 하려는 것이다. 키는 DB 와
 * 다른 곳(.env)에 둔다. 값마다 nonce 를 새로 뽑아, 같은 번호라도 저장값이 매번 달라진다.
 *
 * <p>저장 형식은 {@code v1:base64(nonce || 암호문+태그)} 다. 접두어가 없는 값은 이 기능 이전에
 * 저장된 평문이다. 읽을 수는 있게 두고 {@code PlainPhoneNumberEncryptor} 가 기동 때 암호화한다.
 */
@Component
public class PhoneNumberCipher {

  static final String PREFIX = "v1:";
  private static final String ALGORITHM = "AES/GCM/NoPadding";
  private static final int NONCE_BYTES = 12;
  private static final int TAG_BITS = 128;

  /** JPA 컨버터는 Spring 이 만들지 않을 수 있어(슬라이스 테스트) 여기서 찾는다. */
  private static volatile PhoneNumberCipher current;

  private final SecretKey key;
  private final SecureRandom random = new SecureRandom();

  public PhoneNumberCipher(FestaProperties properties) {
    this.key = toKey(properties.phoneKey());
  }

  @PostConstruct
  void register() {
    current = this;
  }

  static PhoneNumberCipher current() {
    PhoneNumberCipher cipher = current;
    if (cipher == null) {
      throw new IllegalStateException("PhoneNumberCipher 가 초기화되지 않았습니다. 번호를 평문으로 다루지 않습니다.");
    }
    return cipher;
  }

  private static SecretKey toKey(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      throw new IllegalStateException("getit.festa.phone-key(FESTA_PHONE_KEY) 가 없습니다. openssl rand -base64 32");
    }
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(encoded.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("FESTA_PHONE_KEY 는 base64 여야 합니다.", e);
    }
    if (bytes.length != 32) {
      throw new IllegalStateException("FESTA_PHONE_KEY 는 32바이트여야 합니다. 지금 " + bytes.length + "바이트");
    }
    return new SecretKeySpec(bytes, "AES");
  }

  public String encrypt(String plain) {
    if (plain == null) {
      return null;
    }
    try {
      byte[] nonce = new byte[NONCE_BYTES];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
      byte[] sealed = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
      byte[] out = ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array();
      return PREFIX + Base64.getEncoder().encodeToString(out);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("전화번호 암호화 실패", e);
    }
  }

  /** 접두어가 없으면 이전에 저장된 평문이라 그대로 돌려준다. 키가 틀리면 태그 검증에서 실패한다. */
  public String decrypt(String stored) {
    if (stored == null) {
      return null;
    }
    if (!isEncrypted(stored)) {
      return stored;
    }
    try {
      byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, in, 0, NONCE_BYTES));
      byte[] plain = cipher.doFinal(in, NONCE_BYTES, in.length - NONCE_BYTES);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      // 원인 메시지에 저장값이 섞이지 않게 값은 남기지 않는다.
      throw new IllegalStateException("전화번호 복호화 실패. 키가 바뀌었는지 확인", e);
    }
  }

  public static boolean isEncrypted(String stored) {
    return stored != null && stored.startsWith(PREFIX);
  }
}
