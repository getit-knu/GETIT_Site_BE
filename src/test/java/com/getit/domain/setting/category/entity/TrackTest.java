package com.getit.domain.setting.category.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TrackTest {

  @Test
  @DisplayName("이름과 순서로 생성된다")
  void createsTrack() {
    Track track = Track.create("SW", 1);

    assertThat(track.getName()).isEqualTo("SW");
    assertThat(track.getOrder()).isEqualTo(1);
  }

  @Test
  @DisplayName("이름과 순서를 변경한다")
  void updatesTrack() {
    Track track = Track.create("SW", 1);

    track.update("SW 개편", 2);

    assertThat(track.getName()).isEqualTo("SW 개편");
    assertThat(track.getOrder()).isEqualTo(2);
  }
}
