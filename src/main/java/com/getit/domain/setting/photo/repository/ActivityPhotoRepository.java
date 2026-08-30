package com.getit.domain.setting.photo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.getit.domain.setting.photo.entity.ActivityPhoto;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

  List<ActivityPhoto> findAllByOrderByOrderAsc();

  List<ActivityPhoto> findAllByIsVisibleTrueOrderByOrderAsc();
}
