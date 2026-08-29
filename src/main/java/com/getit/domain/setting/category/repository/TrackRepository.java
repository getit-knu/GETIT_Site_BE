package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.Track;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface TrackRepository extends JpaRepository<Track, Long> {

  List<Track> findAllByOrderByOrderAsc();
  Optional<Track> findTopByOrderByOrderDesc();

  /** 10.20 일괄 저장에서 분류 트리를 통째 교체할 때. 동시 CRUD 와 직렬화. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from Track t order by t.order asc")
  List<Track> findAllForUpdate();
}
