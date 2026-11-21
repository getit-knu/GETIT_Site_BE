package com.getit.domain.festa.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * {@code FestaInvestor.phoneNumber} 를 DB 에 암호문으로 쓰고 읽을 때 푼다.
 *
 * <p>암호화 빈을 생성자로 받지 않고 값을 변환할 때 찾는다. {@code @DataJpaTest} 같은 슬라이스
 * 테스트는 모든 엔티티의 컨버터를 만들지만 이 빈은 띄우지 않기 때문이다. 번호를 실제로 읽거나 쓰는데
 * 빈이 없으면 평문으로 흘리지 않고 실패한다.
 */
@Converter
public class PhoneNumberConverter implements AttributeConverter<String, String> {

  @Override
  public String convertToDatabaseColumn(String attribute) {
    return attribute == null ? null : PhoneNumberCipher.current().encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    return dbData == null ? null : PhoneNumberCipher.current().decrypt(dbData);
  }
}
