package com.getit.domain.setting.category.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubCategoryTest {

  @Test
  @DisplayName("이름·순서·소속 트랙으로 생성된다")
  void createsSubCategory() {
    SubCategory subCategory = SubCategory.create("웹기초", 1, 10L);

    assertThat(subCategory.getName()).isEqualTo("웹기초");
    assertThat(subCategory.getOrder()).isEqualTo(1);
    assertThat(subCategory.getTrackId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("이름과 순서를 변경한다 (소속 트랙은 변경하지 않는다)")
  void updatesSubCategory() {
    SubCategory subCategory = SubCategory.create("웹기초", 1, 10L);

    subCategory.update("웹심화", 2);

    assertThat(subCategory.getName()).isEqualTo("웹심화");
    assertThat(subCategory.getOrder()).isEqualTo(2);
    assertThat(subCategory.getTrackId()).isEqualTo(10L);
  }
}
