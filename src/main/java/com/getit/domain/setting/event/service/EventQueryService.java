package com.getit.domain.setting.event.service;

import java.time.YearMonth;
import java.util.List;

public interface EventQueryService {

  List<EventView> findByMonth(int generationNo, YearMonth month);

  List<EventView> findUpcoming(int generationNo);
}
