package com.getit.domain.setting.faq.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.exception.FaqErrorCode;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FaqBulkServiceImplTest {

  @Autowired
  private FaqBulkService faqBulkService;

  @Autowired
  private FaqRepository faqRepository;

  private Faq saved(String question, int order) {
    return faqRepository.save(Faq.create(new FaqCommand(question, "답변", true), order));
  }

  @Test
  @DisplayName("id 있으면 수정 · 없으면 생성 · 리스트에 없는 기존 행은 삭제 · order 는 배열 인덱스")
  void replacesWholeSet() {
    Faq keep = saved("유지", 1);
    Faq gone = saved("삭제될것", 2);

    faqBulkService.replaceAll(List.of(
        new FaqUpsert(null, "신규", "신규답변", false),
        new FaqUpsert(keep.getId(), "수정됨", "수정답변", true)));

    List<Faq> result = faqRepository.findAllByOrderByOrderAscIdAsc();
    assertThat(result).extracting(Faq::getQuestion).containsExactly("신규", "수정됨");
    assertThat(result).extracting(Faq::getOrder).containsExactly(1, 2);
    assertThat(faqRepository.findById(gone.getId())).isEmpty();
    assertThat(result.get(1).isVisible()).isTrue();
    assertThat(result.get(0).isVisible()).isFalse();
  }

  @Test
  @DisplayName("수정은 question·answer·isVisible 을 전부 덮어쓴다")
  void updateOverwritesAllFields() {
    Faq faq = saved("원본", 1);

    faqBulkService.replaceAll(List.of(new FaqUpsert(faq.getId(), "바뀐질문", "바뀐답변", false)));

    Faq updated = faqRepository.findById(faq.getId()).orElseThrow();
    assertThat(updated.getQuestion()).isEqualTo("바뀐질문");
    assertThat(updated.getAnswer()).isEqualTo("바뀐답변");
    assertThat(updated.isVisible()).isFalse();
  }

  @Test
  @DisplayName("생성·삭제 없이 순서만 바꿔도 order 가 배열 인덱스로 재부여된다")
  void reorderOnly() {
    Faq a = saved("A", 1);
    Faq b = saved("B", 2);
    Faq c = saved("C", 3);

    faqBulkService.replaceAll(List.of(
        new FaqUpsert(c.getId(), "C", "답변", true),
        new FaqUpsert(a.getId(), "A", "답변", true),
        new FaqUpsert(b.getId(), "B", "답변", true)));

    assertThat(faqRepository.findAllByOrderByOrderAscIdAsc())
        .extracting(Faq::getQuestion).containsExactly("C", "A", "B");
    assertThat(faqRepository.findById(c.getId()).orElseThrow().getOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("빈 리스트면 전부 삭제한다")
  void emptyListDeletesAll() {
    saved("a", 1);
    saved("b", 2);

    faqBulkService.replaceAll(List.of());

    assertThat(faqRepository.count()).isZero();
  }

  @Test
  @DisplayName("없는 id 를 수정하려 하면 FAQ_NOT_FOUND")
  void unknownIdThrows() {
    assertThatThrownBy(() -> faqBulkService.replaceAll(List.of(
        new FaqUpsert(999L, "q", "a", true))))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FaqErrorCode.FAQ_NOT_FOUND);
  }
}
