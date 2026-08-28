package com.getit.domain.setting.event.service;

import java.util.List;

public interface EventQueryService {

  List<EventView> findByMonth(int generationNo, int year, int month);

  List<EventView> findUpcoming(int generationNo);
}
