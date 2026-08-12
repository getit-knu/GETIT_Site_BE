package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.Track;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {

  List<Track> findAllByOrderByOrderAsc();
}
