package com.getit.domain.setting.category.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.getit.domain.setting.category.entity.Track;
import com.getit.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class TrackRepositoryTest {

  @Autowired
  private TrackRepository trackRepository;

  @Test
  @DisplayName("order 오름차순으로 전체 조회한다")
  void findsAllOrderedByOrder() {
    trackRepository.save(Track.create("창업", 2));
    trackRepository.save(Track.create("SW", 1));

    assertThat(trackRepository.findAllByOrderByOrderAsc())
        .extracting(Track::getName)
        .containsExactly("SW", "창업");
  }

  @Test
  @DisplayName("가장 큰 order 를 가진 트랙 하나를 반환한다")
  void findsTopByOrder() {
    trackRepository.save(Track.create("SW", 1));
    trackRepository.save(Track.create("창업", 3));
    trackRepository.save(Track.create("세미나", 2));

    assertThat(trackRepository.findTopByOrderByOrderDesc())
        .isPresent()
        .get()
        .extracting(Track::getName)
        .isEqualTo("창업");
  }

  @Test
  @DisplayName("트랙이 없으면 빈 Optional 을 반환한다")
  void returnsEmptyWhenNoTrack() {
    assertThat(trackRepository.findTopByOrderByOrderDesc()).isEmpty();
  }
}
