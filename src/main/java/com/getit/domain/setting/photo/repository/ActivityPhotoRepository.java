package com.getit.domain.setting.photo.repository;

import java.util.List;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.getit.domain.setting.photo.entity.ActivityPhoto;

public interface ActivityPhotoRepository extends JpaRepository<ActivityPhoto, Long> {

  List<ActivityPhoto> findAllByOrderByOrderAsc();

  List<ActivityPhoto> findAllByIsVisibleTrueOrderByOrderAsc();

  /**
   * 순서를 바꾸는 경로에서 쓴다. 형제 행을 잠근다.
   *
   * <p>잠그지 않으면 동시 요청이 같은 형제 목록을 읽어 같은 order 를 만들거나 서로의 이동을
   * 덮어쓴다. FAQ 도 같은 이유로 {@code findAllForUpdate} 를 쓴다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from ActivityPhoto p order by p.order asc, p.id asc")
  List<ActivityPhoto> findAllForUpdate();
}
