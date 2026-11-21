package com.getit.domain.festa.service;

import java.util.Optional;

/** festa 도메인이 다른 도메인에 여는 계약. 미설정이면 비어 있다. (이슈 #234) */
public interface FestaScheduleQueryService {

  Optional<FestaScheduleView> findSchedule();
}
