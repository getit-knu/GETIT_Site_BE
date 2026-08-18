package com.getit.domain.lecture.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
public class SubmissionTypeSetConverter implements AttributeConverter<Set<SubmissionType>, String> {

  @Override
  public String convertToDatabaseColumn(Set<SubmissionType> attribute) {
    if (attribute == null) {
      return null;
    }
    return attribute.stream().map(Enum::name).collect(Collectors.joining(","));
  }

  @Override
  public Set<SubmissionType> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return Set.of();
    }
    return Arrays.stream(dbData.split(","))
        .map(SubmissionType::valueOf)
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
