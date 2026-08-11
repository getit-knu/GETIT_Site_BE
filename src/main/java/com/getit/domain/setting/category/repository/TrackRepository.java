package com.getit.domain.setting.category.repository;

import com.getit.domain.setting.category.entity.Track;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository extends JpaRepository<Track, Long> {
}
