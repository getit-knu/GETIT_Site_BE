package com.getit.domain.setting.faq.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class FaqRepositoryTest {

  @Autowired
  private FaqRepository faqRepository;

  private Faq faq(String question, int order) {
    return Faq.create(new FaqCommand(question, "답변입니다.", true), order);
  }

  @Test
  @DisplayName("order 오름차순으로 조회한다")
  void findsAllOrderByOrderAscIdAsc() {
    faqRepository.save(faq("B", 2));
    faqRepository.save(faq("A", 1));

    assertThat(faqRepository.findAllByOrderByOrderAscIdAsc())
        .extracting(Faq::getQuestion)
        .containsExactly("A", "B");
  }

  @Test
  @DisplayName("order 가 같으면 id 오름차순으로 정렬한다")
  void tieBreaksById() {
    Faq first = faqRepository.save(faq("A", 1));
    Faq second = faqRepository.save(faq("B", 1));

    assertThat(faqRepository.findAllByOrderByOrderAscIdAsc())
        .extracting(Faq::getId)
        .containsExactly(first.getId(), second.getId());
  }
}
