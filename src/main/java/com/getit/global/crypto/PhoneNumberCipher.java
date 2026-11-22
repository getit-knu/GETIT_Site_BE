package com.getit.global.crypto;

import jakarta.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 전화번호를 AES-256-GCM 으로 암호화한다. 부원·지원서·FESTA 번호가 함께 쓴다. (이슈 #239)
 *
 * <p>DB 덤프나 백업, 읽기 권한이 새어도 키 없이는 번호를 읽을 수 없게 하려는 것이다. 키는 DB 와
 * 다른 곳(.env)에 둔다. 값마다 nonce 를 새로 뽑아, 같은 번호라도 저장값이 매번 달라진다. 그래서 번호로
 * 검색하거나 중복을 확인할 수 없다 — 지금 그런 곳은 없다.
 *
 * <p>저장 형식은 {@code v1:base64(nonce || 암호문+태그)} 다. 접두어가 없는 값은 암호화 이전에
 * 저장된 평문이다. 읽을 수는 있게 두고 {@link PlainPhoneNumberEncryptor} 가 기동 때 암호화한다.
 *
 * <p><b>키 교체</b>: 새 키를 {@code phone-key} 에, 이전 키를 {@code phone-key-previous} 에 두고
 * 재시작하면, 지금 키로 풀리지 않는 값은 이전 키로 풀고 기동 때 새 키로 다시 암호화한다.
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
  /** 키 교체 중에만 있다. 없으면 null. */
  private final SecretKey previousKey;
  private final SecureRandom random = new SecureRandom();

  public PhoneNumberCipher(
      @Value("${getit.crypto.phone-key:}") String encodedKey,
      @Value("${getit.crypto.phone-key-previous:}") String encodedPreviousKey
  ) {
    if (encodedKey == null || encodedKey.isBlank()) {
      throw new IllegalStateException("getit.crypto.phone-key(FESTA_PHONE_KEY) 가 없습니다. openssl rand -base64 32");
    }
    this.key = toKey(encodedKey, "FESTA_PHONE_KEY");
    this.previousKey = encodedPreviousKey == null || encodedPreviousKey.isBlank()
        ? null
        : toKey(encodedPreviousKey, "FESTA_PHONE_KEY_PREVIOUS");
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

  private static SecretKey toKey(String encoded, String name) {
    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(encoded.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(name + " 는 base64 여야 합니다.", e);
    }
    if (bytes.length != 32) {
      throw new IllegalStateException(name + " 는 32바이트여야 합니다. 지금 " + bytes.length + "바이트");
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

  /**
   * 접두어가 없으면 이전에 저장된 평문이라 그대로 돌려준다. 지금 키로 풀리지 않으면 이전 키로 시도하고,
   * 둘 다 안 되면 실패한다.
   */
  public String decrypt(String stored) {
    if (stored == null) {
      return null;
    }
    if (!isEncrypted(stored)) {
      return stored;
    }
    try {
      byte[] in = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
      try {
        return open(key, in);
      } catch (AEADBadTagException wrongKey) {
        if (previousKey == null) {
          throw wrongKey;
        }
        return open(previousKey, in);
      }
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      // 원인 메시지에 저장값이 섞이지 않게 값은 남기지 않는다.
      throw new IllegalStateException("전화번호 복호화 실패. 키가 바뀌었는지 확인", e);
    }
  }

  /** 키 교체 중인지. 아니면 이미 암호화된 값은 다시 볼 필요가 없다. */
  public boolean rotating() {
    return previousKey != null;
  }

  /** 평문이거나, 지금 키가 아닌 키로 암호화돼 있어 다시 암호화해야 하는지. */
  public boolean needsEncryption(String stored) {
    if (stored == null) {
      return false;
    }
    if (!isEncrypted(stored)) {
      return true;
    }
    try {
      open(key, Base64.getDecoder().decode(stored.substring(PREFIX.length())));
      return false;
    } catch (GeneralSecurityException | IllegalArgumentException e) {
      return true;
    }
  }

  private static String open(SecretKey secret, byte[] in) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance(ALGORITHM);
    cipher.init(Cipher.DECRYPT_MODE, secret, new GCMParameterSpec(TAG_BITS, in, 0, NONCE_BYTES));
    byte[] plain = cipher.doFinal(in, NONCE_BYTES, in.length - NONCE_BYTES);
    return new String(plain, StandardCharsets.UTF_8);
  }

  public static boolean isEncrypted(String stored) {
    return stored != null && stored.startsWith(PREFIX);
  }
}
