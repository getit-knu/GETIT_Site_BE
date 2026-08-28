package com.getit.domain.setting.faq.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.dto.FaqRequest;
import com.getit.domain.setting.faq.dto.FaqResult;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.exception.FaqErrorCode;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class FaqAdminServiceTest {

  @Autowired
  private FaqAdminService faqAdminService;

  @Autowired
  private FaqRepository faqRepository;

  private FaqRequest request(String question, Integer order) {
    return new FaqRequest(question, "답변입니다.", true, order);
  }

  private Faq saved(String question, int order) {
    return faqRepository.save(Faq.create(new FaqCommand(question, "답변입니다.", true), order));
  }

  private int orderOf(Long id) {
    return faqRepository.findById(id).orElseThrow().getOrder();
  }

  @Nested
  @DisplayName("getFaqs")
  class GetFaqs {

    @Test
    @DisplayName("order 순으로 반환한다")
    void returnsFaqsInOrder() {
      saved("B", 2);
      saved("A", 1);

      List<FaqResult> results = faqAdminService.getFaqs();

      assertThat(results).extracting(FaqResult::question).containsExactly("A", "B");
    }
  }

  @Nested
  @DisplayName("createFaq")
  class CreateFaq {

    @Test
    @DisplayName("첫 FAQ 를 추가하면 order 는 1이다")
    void createsFirstFaq() {
      FaqResult result = faqAdminService.createFaq(request("가입 조건은?", 1));

      assertThat(result.question()).isEqualTo("가입 조건은?");
      assertThat(result.order()).isEqualTo(1);
    }

    @Test
    @DisplayName("order 를 생략하면 맨 뒤에 붙는다")
    void appendsWhenOrderOmitted() {
      saved("A", 1);

      FaqResult result = faqAdminService.createFaq(request("B", null));

      assertThat(result.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("요청 order 가 기존 개수+1 보다 크면 맨 뒤로 clamp 된다")
    void clampsOrderAboveValidRange() {
      saved("A", 1);

      FaqResult result = faqAdminService.createFaq(request("B", 99));

      assertThat(result.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("중간에 끼워 넣으면 그 뒤 항목들의 순서를 한 칸씩 민다")
    void shiftsExistingItemsWhenInsertingInMiddle() {
      Faq first = saved("A", 1);
      Faq second = saved("B", 2);

      FaqResult result = faqAdminService.createFaq(request("C", 1));

      assertThat(result.order()).isEqualTo(1);
      assertThat(orderOf(first.getId())).isEqualTo(2);
      assertThat(orderOf(second.getId())).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("updateFaq")
  class UpdateFaq {

    @Test
    @DisplayName("order 를 생략하면 순서는 그대로다")
    void keepsOrderWhenOmitted() {
      Faq target = saved("A", 1);
      saved("B", 2);

      FaqResult result = faqAdminService.updateFaq(target.getId(), request("변경", null));

      assertThat(result.question()).isEqualTo("변경");
      assertThat(result.order()).isEqualTo(1);
    }

    @Test
    @DisplayName("앞으로 옮기면 그 사이 항목들이 한 칸씩 뒤로 밀린다")
    void movesUpAndShiftsBetweenItemsBack() {
      Faq first = saved("A", 1);
      Faq second = saved("B", 2);
      Faq third = saved("C", 3);

      faqAdminService.updateFaq(third.getId(), request("C", 1));

      assertThat(orderOf(third.getId())).isEqualTo(1);
      assertThat(orderOf(first.getId())).isEqualTo(2);
      assertThat(orderOf(second.getId())).isEqualTo(3);
    }

    @Test
    @DisplayName("뒤로 옮기면 그 사이 항목들이 한 칸씩 앞으로 당겨진다")
    void movesDownAndShiftsBetweenItemsForward() {
      Faq first = saved("A", 1);
      Faq second = saved("B", 2);
      Faq third = saved("C", 3);

      faqAdminService.updateFaq(first.getId(), request("A", 3));

      assertThat(orderOf(first.getId())).isEqualTo(3);
      assertThat(orderOf(second.getId())).isEqualTo(1);
      assertThat(orderOf(third.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("요청 order 가 전체 개수보다 크면 마지막 순번으로 clamp 된다")
    void clampsOrderAboveValidRange() {
      Faq first = saved("A", 1);
      saved("B", 2);

      FaqResult result = faqAdminService.updateFaq(first.getId(), request("A", 99));

      assertThat(result.order()).isEqualTo(2);
    }

    @Test
    @DisplayName("없는 FAQ 면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> faqAdminService.updateFaq(999L, request("A", 1)))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(FaqErrorCode.FAQ_NOT_FOUND);
    }
  }

  @Nested
  @DisplayName("deleteFaq")
  class DeleteFaq {

    @Test
    @DisplayName("FAQ 를 삭제하고 뒤 순번을 한 칸씩 당긴다")
    void deletesAndShiftsRemainingOrder() {
      Faq first = saved("A", 1);
      Faq second = saved("B", 2);

      faqAdminService.deleteFaq(first.getId());

      assertThat(faqRepository.findById(first.getId())).isEmpty();
      assertThat(orderOf(second.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("없는 FAQ 면 예외가 발생한다")
    void throwsWhenNotFound() {
      assertThatThrownBy(() -> faqAdminService.deleteFaq(999L))
          .isInstanceOf(BusinessException.class)
          .extracting("errorCode")
          .isEqualTo(FaqErrorCode.FAQ_NOT_FOUND);
    }
  }
}
