package com.getit.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

/**
 * 어떤 동작이 SQL 을 몇 번 보내는지 센다.
 *
 * <p>N+1 은 결과가 맞는 채로 쿼리 수만 늘어난다. 그래서 값만 단언하는 테스트는 배치 조회를
 * 단건 루프로 되돌려도 그대로 통과한다. 쿼리 수를 직접 고정해야 그 회귀가 잡힌다.
 *
 * <p>세기 전에 영속성 컨텍스트를 비운다. 비우지 않으면 두 가지가 섞인다 — 픽스처가 저장한
 * 엔티티의 INSERT 가 동작 중에 플러시되면서 함께 세어지고, 이미 1차 캐시에 있는 엔티티는
 * 단건 조회를 해도 SQL 이 나가지 않아 N+1 이 가려진다.
 */
public final class QueryCounter {

  private final EntityManager entityManager;
  private final Statistics statistics;

  private QueryCounter(EntityManager entityManager, Statistics statistics) {
    this.entityManager = entityManager;
    this.statistics = statistics;
  }

  public static QueryCounter of(EntityManagerFactory entityManagerFactory, EntityManager entityManager) {
    return new QueryCounter(
        entityManager,
        entityManagerFactory.unwrap(SessionFactory.class).getStatistics());
  }

  /** {@code action} 이 보낸 SQL 문장 수. 조회·삽입·수정을 모두 센다. */
  public long countDuring(Runnable action) {
    entityManager.flush();
    entityManager.clear();
    statistics.clear();
    action.run();
    return statistics.getPrepareStatementCount();
  }
}
