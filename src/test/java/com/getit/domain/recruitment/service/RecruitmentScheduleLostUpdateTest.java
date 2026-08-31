package com.getit.domain.recruitment.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.RecruitmentSchedule;
import com.getit.domain.recruitment.repository.RecruitmentScheduleRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 지원 스위치와 일정 저장이 서로를 덮지 않는지 본다. (PR #173 리뷰 지적)
 *
 * <p>이 행에는 성격이 다른 두 갈래의 쓰기가 붙는다 — 일정 저장(6.2 · 홈 일괄 저장)과
 * 지원 스위치다. Hibernate 의 기본 UPDATE 는 바꾸지 않은 컬럼까지 함께 쓰기 때문에, 일정
 * 저장이 행을 읽어 둔 뒤 그 사이 스위치가 내려가면, 일정 저장이 커밋될 때 스위치가 자기가
 * 읽어 둔 옛 값(올라감)으로 되돌아간다. <b>사고 대응용 스위치가 조용히 풀린다.</b>
 *
 * <p>순차 호출로는 재현되지 않는다 — 나중 트랜잭션이 어차피 최신 값을 읽기 때문이다.
 * 그래서 두 트랜잭션을 겹쳐 놓고 순서를 래치로 고정한다. 클래스에 {@code @Transactional} 을
 * 걸지 않는 이유도 같다. 커밋이 실제로 일어나야 이 문제가 드러난다.
 */
@SpringBootTest
class RecruitmentScheduleLostUpdateTest {

  private static final int TIMEOUT_SECONDS = 10;

  @Autowired
  private RecruitmentScheduleService recruitmentScheduleService;

  @Autowired
  private RecruitmentScheduleRepository recruitmentScheduleRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private Long generationId;

  private LocalDateTime dt(int month, int day) {
    return LocalDateTime.of(2026, month, day, 0, 0);
  }

  @BeforeEach
  void setUp() {
    cleanUp();
    Generation generation = Generation.create(9, 2026);
    generation.activate();
    generationId = generationRepository.save(generation).getId();

    recruitmentScheduleRepository.save(RecruitmentSchedule.create(
        generationId, dt(9, 1), dt(9, 30), dt(9, 1), dt(9, 10), dt(9, 15)));
  }

  @AfterEach
  void cleanUp() {
    recruitmentScheduleRepository.deleteAll();
    generationRepository.deleteAll();
  }

  private TransactionTemplate tx() {
    return new TransactionTemplate(transactionManager);
  }

  @Test
  @DisplayName("일정 저장 도중에 스위치를 내리면, 일정 저장이 커밋돼도 스위치는 내려간 채 남는다")
  void scheduleSaveDoesNotResetTheSwitch() throws Exception {
    CountDownLatch loadedByScheduleSave = new CountDownLatch(1);
    CountDownLatch switchTurnedOff = new CountDownLatch(1);
    ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      // 일정 저장 트랜잭션. 행을 먼저 읽어 두고(이때 스위치는 올라가 있다) 기다린다.
      Future<?> scheduleSave = executor.submit(() -> tx().execute(status -> {
        RecruitmentSchedule schedule =
            recruitmentScheduleRepository.findByGenerationId(generationId).orElseThrow();
        loadedByScheduleSave.countDown();
        awaitOrFail(switchTurnedOff);

        schedule.update(dt(10, 1), dt(10, 30), dt(10, 1), dt(10, 10), dt(10, 15));
        return null;
      }));

      awaitOrFail(loadedByScheduleSave);
      tx().execute(status -> recruitmentScheduleService.changeApplyEnabled(false));
      switchTurnedOff.countDown();
      scheduleSave.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    RecruitmentSchedule saved =
        recruitmentScheduleRepository.findByGenerationId(generationId).orElseThrow();
    assertThat(saved.isApplyEnabled()).isFalse();
    // 일정 저장 자체는 정상으로 반영돼야 한다. 스위치를 지키느라 일정을 잃으면 안 된다.
    assertThat(saved.getDocumentEndAt()).isEqualTo(dt(10, 10));
  }

  private void awaitOrFail(CountDownLatch latch) {
    try {
      if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new IllegalStateException("다른 트랜잭션을 기다리다 시간이 초과됐다");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
