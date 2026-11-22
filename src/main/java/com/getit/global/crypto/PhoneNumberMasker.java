package com.getit.global.crypto;

/**
 * 목록 응답에 싣는 가린 번호. 앞 3자리와 끝 4자리 숫자만 남긴다. (이슈 #237)
 *
 * <p>{@code 010-1234-5678} → {@code 010-****-5678}. 부원이 직접 고친 번호는 형식이 제각각일 수 있어
 * 구분자는 그대로 두고 숫자만 가린다. 숫자가 7개 이하면 전부 가린다.
 */
public final class PhoneNumberMasker {

  private static final int KEEP_HEAD = 3;
  private static final int KEEP_TAIL = 4;

  private PhoneNumberMasker() {
  }

  public static String mask(String phoneNumber) {
    if (phoneNumber == null) {
      return null;
    }
    long digits = phoneNumber.chars().filter(Character::isDigit).count();
    boolean maskAll = digits <= KEEP_HEAD + KEEP_TAIL;
    StringBuilder out = new StringBuilder(phoneNumber.length());
    int seen = 0;
    for (char c : phoneNumber.toCharArray()) {
      if (!Character.isDigit(c)) {
        out.append(c);
        continue;
      }
      boolean keep = !maskAll && (seen < KEEP_HEAD || seen >= digits - KEEP_TAIL);
      out.append(keep ? c : '*');
      seen++;
    }
    return out.toString();
  }
}
