package com.getit.domain.setting.faq.service;

import com.getit.domain.setting.faq.repository.FaqRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FaqQueryServiceImpl implements FaqQueryService {

  private final FaqRepository faqRepository;

  @Override
  public List<FaqView> findVisible() {
    return faqRepository.findByIsVisibleTrueOrderByOrderAscIdAsc().stream()
        .map(faq -> new FaqView(faq.getId(), faq.getQuestion(), faq.getAnswer(), faq.getOrder()))
        .toList();
  }
}
