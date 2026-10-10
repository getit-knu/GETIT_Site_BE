package com.getit.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.recruitment.entity.Application;
import com.getit.domain.recruitment.repository.ApplicationRepository;
import com.getit.domain.setting.generation.entity.Generation;
import com.getit.domain.setting.generation.repository.GenerationRepository;
import com.getit.domain.user.entity.College;
import com.getit.domain.user.entity.Major;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.repository.CollegeRepository;
import com.getit.domain.user.repository.MajorRepository;
import com.getit.domain.user.repository.UserRepository;
import com.getit.support.QueryCounter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * 합격자 승격이 후보 수에 비례해 마스터 데이터를 다시 읽지 않는지 고정한다.
 *
 * <p>커밋 {@code 6b2552b} 에서 College · Major 이름 조회를 후보별 {@code findById} 에서
 * {@code findAllById} + Map 으로 바꿨다. 그때 쿼리 수를 확인하는 테스트가 없었으므로, 지금
 * 루프 안 단건 조회로 되돌려도 기존 테스트는 전부 통과한다.
 *
 * <p>절대 쿼리 수가 아니라 <b>증가분</b>을 단언한다. 승격은 후보마다 UPDATE 가 한 번씩 나가므로
 * 총 쿼리 수는 후보 수에 비례해 늘어나는 것이 정상이다. 늘어나면 안 되는 것은 그 UPDATE 외의
 * 조회다. 고정 비용(활성 기수 · 지원서 목록 · College · Major)은 두 측정에서 똑같이 빠지므로,
 * 증가분이 후보 수 증가분과 같으면 후보당 추가 조회가 없다는 뜻이 된다.
 *
 * <p>후보마다 <b>서로 다른</b> College · Major 를 붙인다. 모두 같은 값을 쓰면 단건 조회로
 * 되돌려도 두 번째부터는 1차 캐시에 걸려 SQL 이 나가지 않아 회귀가 가려진다.
 */
@SpringBootTest
@Transactional
class UserPromotionQueryCountTest {

  private static final int MANY = 20;

  @Autowired
  private UserPromotionService userPromotionService;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ApplicationRepository applicationRepository;

  @Autowired
  private GenerationRepository generationRepository;

  @Autowired
  private CollegeRepository collegeRepository;

  @Autowired
  private MajorRepository majorRepository;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private EntityManager entityManager;

  private QueryCounter queryCounter;
  private Generation generation;

  @BeforeEach
  void setUp() {
    queryCounter = QueryCounter.of(entityManagerFactory, entityManager);
    Generation created = Generation.create(9, 2026);
    created.activate();
    generation = generationRepository.save(created);
  }

  @Test
  @DisplayName("승격 후보가 늘어도 후보당 추가 조회가 생기지 않는다")
  void doesNotQueryPerCandidate() {
    List<Long> applicationIds = createFinalPassCandidates(1 + MANY);
    List<Long> one = applicationIds.subList(0, 1);
    List<Long> many = applicationIds.subList(1, 1 + MANY);

    long queriesForOne = queryCounter.countDuring(
        () -> userPromotionService.promote(generation.getId(), one));
    long queriesForMany = queryCounter.countDuring(
        () -> userPromotionService.promote(generation.getId(), many));

    assertThat(queriesForMany - queriesForOne)
        .as("""
            후보가 %d 명 늘었으면 늘어야 하는 쿼리는 승격 UPDATE %d 개뿐이다.
            이보다 크면 후보마다 조회가 한 번씩 더 나가고 있다 (N+1).
            측정값 — 후보 1명: %d 회, 후보 %d 명: %d 회"""
            .formatted(MANY - 1, MANY - 1, queriesForOne, MANY, queriesForMany))
        .isEqualTo(MANY - 1);
  }

  /** 후보마다 서로 다른 College · Major 를 가진 FINAL_PASS 지원서를 만들고 그 id 를 돌려준다. */
  private List<Long> createFinalPassCandidates(int count) {
    List<Long> applicationIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      College college = collegeRepository.save(College.create("단과대학-" + i));
      Major major = majorRepository.save(Major.create(college.getId(), "학과-" + i));
      User user = userRepository.save(
          User.createGuest("google-" + i, "user" + i + "@getit.com", "지원자" + i, "url"));

      Application application = applicationRepository.save(Application.createDraft(
          user.getId(), generation.getId(), "지원자" + i, "user" + i + "@getit.com",
          "010-1234-5678", college.getId(), major.getId(), 3, "20211%05d".formatted(i)));
      application.submit(LocalDateTime.now(), LocalDateTime.now());
      application.decideDocumentResult(true);
      application.decideFinalResult(true);
      applicationIds.add(application.getId());
    }
    return applicationIds;
  }
}
