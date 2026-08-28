package com.getit.domain.project.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TechStackListConverterTest {

  private final TechStackListConverter converter = new TechStackListConverter();

  @Test
  @DisplayName("리스트 ↔ 콤마 문자열 왕복")
  void roundTrip() {
    assertThat(converter.convertToDatabaseColumn(List.of("React", "Spring"))).isEqualTo("React,Spring");
    assertThat(converter.convertToEntityAttribute("React,Spring")).containsExactly("React", "Spring");
  }

  @Test
  @DisplayName("null · 빈 값 처리")
  void nullAndEmpty() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull();
    assertThat(converter.convertToDatabaseColumn(List.of())).isNull();
    assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    assertThat(converter.convertToEntityAttribute("")).isEmpty();
  }

  @Test
  @DisplayName("스택 이름에 쉼표가 있으면 저장을 거부한다")
  void rejectsComma() {
    assertThatThrownBy(() -> converter.convertToDatabaseColumn(List.of("React, Redux")))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
