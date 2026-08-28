package com.getit.domain.setting.faq.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.faq.dto.FaqCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FaqTest {

  private FaqCommand command(String question, String answer, boolean isVisible) {
    return new FaqCommand(question, answer, isVisible);
  }

  @Test
  @DisplayName("생성한다")
  void creates() {
    Faq faq = Faq.create(command("가입 조건은?", "재학생이면 누구나 가능합니다.", true), 1);

    assertThat(faq.getOrder()).isEqualTo(1);
    assertThat(faq.getQuestion()).isEqualTo("가입 조건은?");
    assertThat(faq.getAnswer()).isEqualTo("재학생이면 누구나 가능합니다.");
    assertThat(faq.isVisible()).isTrue();
  }

  @Test
  @DisplayName("수정한다")
  void updates() {
    Faq faq = Faq.create(command("가입 조건은?", "재학생이면 누구나 가능합니다.", true), 1);

    faq.update(command("활동 기간은?", "한 학기입니다.", false));

    assertThat(faq.getQuestion()).isEqualTo("활동 기간은?");
    assertThat(faq.getAnswer()).isEqualTo("한 학기입니다.");
    assertThat(faq.isVisible()).isFalse();
    assertThat(faq.getOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("순서를 변경한다")
  void updatesOrder() {
    Faq faq = Faq.create(command("가입 조건은?", "재학생이면 누구나 가능합니다.", true), 1);

    faq.updateOrder(3);

    assertThat(faq.getOrder()).isEqualTo(3);
  }
}
