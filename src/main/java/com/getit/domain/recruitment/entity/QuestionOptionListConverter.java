package com.getit.domain.recruitment.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.getit.domain.recruitment.dto.QuestionOption;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * {@code options} 컬럼(json)과 {@code List<QuestionOption>} 사이를 변환한다.
 *
 * <p>TEXT 타입 질문은 options 가 없어 null 을 그대로 저장 · 반환한다.
 */
@Converter
public class QuestionOptionListConverter implements AttributeConverter<List<QuestionOption>, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<QuestionOption> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("options 직렬화에 실패했습니다.", e);
    }
  }

  @Override
  public List<QuestionOption> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<QuestionOption>>() { });
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("options 역직렬화에 실패했습니다.", e);
    }
  }
}
