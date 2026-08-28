package com.getit.domain.project.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Converter
public class TechStackListConverter implements AttributeConverter<List<String>, String> {

  @Override
  public String convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return null;
    }
    if (attribute.stream().anyMatch(stack -> stack.contains(","))) {
      throw new IllegalArgumentException("기술 스택 이름에는 쉼표를 쓸 수 없습니다.");
    }
    return String.join(",", attribute);
  }

  @Override
  public List<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return List.of();
    }
    return Arrays.stream(dbData.split(",")).map(String::trim).collect(Collectors.toList());
  }
}
