package com.getit.domain.setting.faq.service;

import java.util.List;

public interface FaqBulkService {

  void replaceAll(List<FaqUpsert> desired);
}
