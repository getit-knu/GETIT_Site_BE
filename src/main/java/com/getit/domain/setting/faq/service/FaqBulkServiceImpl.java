package com.getit.domain.setting.faq.service;

import com.getit.domain.setting.faq.dto.FaqCommand;
import com.getit.domain.setting.faq.entity.Faq;
import com.getit.domain.setting.faq.exception.FaqErrorCode;
import com.getit.domain.setting.faq.repository.FaqRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class FaqBulkServiceImpl implements FaqBulkService {

  private final FaqRepository faqRepository;

  @Override
  public void replaceAll(List<FaqUpsert> desired) {
    Map<Long, Faq> existing = faqRepository.findAllForUpdate().stream()
        .collect(Collectors.toMap(Faq::getId, Function.identity()));
    Set<Long> keepIds = desired.stream()
        .map(FaqUpsert::id)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    faqRepository.deleteAll(existing.values().stream()
        .filter(faq -> !keepIds.contains(faq.getId()))
        .toList());

    for (int i = 0; i < desired.size(); i++) {
      FaqUpsert item = desired.get(i);
      FaqCommand command = new FaqCommand(item.question(), item.answer(), item.isVisible());
      int order = i + 1;
      if (item.id() == null) {
        faqRepository.save(Faq.create(command, order));
        continue;
      }
      Faq faq = existing.get(item.id());
      if (faq == null) {
        throw new BusinessException(FaqErrorCode.FAQ_NOT_FOUND);
      }
      faq.update(command);
      faq.updateOrder(order);
    }
  }
}
