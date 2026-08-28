package com.getit.domain.setting.faq.service;

import com.getit.domain.setting.faq.dto.FaqRequest;
import com.getit.domain.setting.faq.dto.FaqResult;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.exception.FaqErrorCode;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FAQ 조회 · 저장. (API 명세서 10.18 ~ 10.19)
 *
 * <p>{@code Curriculum} 과 달리 기수 스코프가 없다 — FAQ 는 지원 관련 상시 문답이라 기수가 바뀌어도
 * 유지된다(명세서 요청 · 응답에 generationId 없음).
 *
 * <p>{@code order} 는 클라이언트가 직접 값을 보내지만(별도 순서 변경 엔드포인트 없음), 1..N 연속
 * 불변식은 이 서비스가 책임진다(PR #78 Copilot 리뷰 지적, {@code CurriculumAdminService} 와 동일).
 * 요청 order 가 null 이면 생성은 맨 뒤에 붙이고, 수정은 순서를 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqAdminService {

  private final FaqRepository faqRepository;

  /** 10.18. */
  public List<FaqResult> getFaqs() {
    return faqRepository.findAllByOrderByOrderAscIdAsc().stream()
        .map(FaqResult::from)
        .toList();
  }

  /** 10.19 POST. order 생략 시 맨 뒤, 값이 있으면 [1, 기존 개수+1] 로 clamp 한 뒤 이후 항목을 뒤로 민다. */
  @Transactional
  public FaqResult createFaq(FaqRequest request) {
    List<Faq> siblings = faqRepository.findAllByOrderByOrderAscIdAsc();

    int newOrder = request.order() == null
        ? siblings.size() + 1
        : clamp(request.order(), 1, siblings.size() + 1);
    siblings.stream()
        .filter(sibling -> sibling.getOrder() >= newOrder)
        .forEach(sibling -> sibling.updateOrder(sibling.getOrder() + 1));

    Faq saved = faqRepository.save(Faq.create(request.toCommand(), newOrder));

    return FaqResult.from(saved);
  }

  /**
   * 10.19 PUT. order 가 null 이면 순서를 유지하고, 값이 있으면 [1, 전체 개수] 로 clamp 한 뒤 그
   * 구간의 다른 항목을 밀거나 당겨서 자리를 만든다.
   */
  @Transactional
  public FaqResult updateFaq(Long faqId, FaqRequest request) {
    Faq target = findFaq(faqId);

    if (request.order() != null) {
      moveOrder(target, request.order());
    }
    target.update(request.toCommand());

    return FaqResult.from(target);
  }

  /** 10.19 DELETE. 삭제된 순번 뒤 항목을 한 칸씩 당겨 order 결번을 막는다. */
  @Transactional
  public void deleteFaq(Long faqId) {
    Faq faq = findFaq(faqId);
    int deletedOrder = faq.getOrder();

    faqRepository.delete(faq);

    faqRepository.findAllByOrderByOrderAscIdAsc().stream()
        .filter(sibling -> sibling.getOrder() > deletedOrder)
        .forEach(sibling -> sibling.updateOrder(sibling.getOrder() - 1));
  }

  private void moveOrder(Faq target, int requestedOrder) {
    List<Faq> siblings = faqRepository.findAllByOrderByOrderAscIdAsc();
    int currentOrder = target.getOrder();
    int newOrder = clamp(requestedOrder, 1, siblings.size());
    if (newOrder < currentOrder) {
      siblings.stream()
          .filter(sibling -> !sibling.getId().equals(target.getId()))
          .filter(sibling -> sibling.getOrder() >= newOrder && sibling.getOrder() < currentOrder)
          .forEach(sibling -> sibling.updateOrder(sibling.getOrder() + 1));
    } else if (newOrder > currentOrder) {
      siblings.stream()
          .filter(sibling -> !sibling.getId().equals(target.getId()))
          .filter(sibling -> sibling.getOrder() > currentOrder && sibling.getOrder() <= newOrder)
          .forEach(sibling -> sibling.updateOrder(sibling.getOrder() - 1));
    }
    target.updateOrder(newOrder);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(value, max));
  }

  private Faq findFaq(Long faqId) {
    return faqRepository.findById(faqId)
        .orElseThrow(() -> new BusinessException(FaqErrorCode.FAQ_NOT_FOUND));
  }
}
