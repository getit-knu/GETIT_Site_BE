package com.getit.domain.festa.service;

import com.getit.domain.festa.dto.FestaScheduleRequest;
import com.getit.domain.festa.dto.FestaScheduleResult;
import com.getit.domain.festa.entity.FestaSchedule;
import com.getit.domain.festa.exception.FestaErrorCode;
import com.getit.domain.festa.repository.FestaScheduleRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FESTA 투표 기간 관리. 일정이 없으면 토글만으로 동작한다. (이슈 #234) */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FestaScheduleService implements FestaScheduleQueryService {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private final FestaScheduleRepository scheduleRepository;

  public FestaScheduleResult getSchedule() {
    return findSchedule()
        .map(view -> new FestaScheduleResult(view.startAt(), view.endAt()))
        .orElse(new FestaScheduleResult(null, null));
  }

  @Transactional
  public FestaScheduleResult saveSchedule(FestaScheduleRequest request) {
    if (!request.startAt().isBefore(request.endAt())) {
      throw new BusinessException(FestaErrorCode.INVALID_FESTA_SCHEDULE);
    }
    LocalDateTime start = toSeoul(request.startAt());
    LocalDateTime end = toSeoul(request.endAt());
    scheduleRepository.findById(FestaSchedule.SINGLETON_ID).ifPresentOrElse(
        schedule -> schedule.change(start, end),
        () -> scheduleRepository.save(FestaSchedule.of(start, end)));
    scheduleRepository.flush();
    return getSchedule();
  }

  @Override
  public Optional<FestaScheduleView> findSchedule() {
    return scheduleRepository.findById(FestaSchedule.SINGLETON_ID)
        .map(schedule -> new FestaScheduleView(toOffset(schedule.getStartAt()), toOffset(schedule.getEndAt())));
  }

  private static LocalDateTime toSeoul(OffsetDateTime value) {
    return value.atZoneSameInstant(SEOUL).toLocalDateTime();
  }

  private static OffsetDateTime toOffset(LocalDateTime value) {
    return value.atZone(SEOUL).toOffsetDateTime();
  }
}
