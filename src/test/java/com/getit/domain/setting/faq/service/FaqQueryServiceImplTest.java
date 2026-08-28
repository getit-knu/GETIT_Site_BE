package com.getit.domain.setting.faq.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.repository.FaqRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FaqQueryServiceImplTest {

  @Autowired
  private FaqQueryService faqQueryService;

  @Autowired
  private FaqRepository faqRepository;

  private void save(String question, boolean visible, int order) {
    faqRepository.save(Faq.create(new FaqCommand(question, "답변", visible), order));
  }

  @Test
  @DisplayName("노출 FAQ 만 order 순으로 반환한다")
  void returnsVisibleInOrder() {
    save("B", true, 2);
    save("A", true, 1);
    save("숨김", false, 3);

    List<FaqView> result = faqQueryService.findVisible();

    assertThat(result).extracting(FaqView::question).containsExactly("A", "B");
  }
}
