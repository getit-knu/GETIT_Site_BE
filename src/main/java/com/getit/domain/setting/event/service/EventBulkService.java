package com.getit.domain.setting.event.service;

import java.util.List;

public interface EventBulkService {

  void replaceAll(int generationNo, List<EventUpsert> desired);
}
