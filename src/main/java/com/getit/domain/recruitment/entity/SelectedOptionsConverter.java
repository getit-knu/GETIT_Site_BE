package com.getit.domain.recruitment.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.List;

/**
 * {@code selected_options} 컬럼(json)과 {@code List<String>} 사이를 변환한다.
 *
 * <p>CHOICE · CHECKBOX 답변의 선택지 id 목록이다. TEXT 답변은 null 을 그대로 저장 · 반환한다.
 * {@code QuestionOptionListConverter} 와 같은 패턴이다.
 */
@Converter
public class SelectedOptionsConverter implements AttributeConverter<List<String>, String> {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.writeValueAsString(attribute);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("selectedOptions 직렬화에 실패했습니다.", e);
    }
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      return OBJECT_MAPPER.readValue(dbData, new TypeReference<List<String>>() { });
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("selectedOptions 역직렬화에 실패했습니다.", e);
    }
  }
}
