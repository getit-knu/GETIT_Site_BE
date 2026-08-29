package com.getit.domain.file.repository;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

  Optional<FileAsset> findByIdAndDeletedAtIsNull(Long id);

  /** 아직 어디에도 연결되지 않은 채 오래 남아 있는 파일. 정리 배치가 쓴다. */
  List<FileAsset> findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
      FileStatus status, LocalDateTime threshold);
  List<FileAsset> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

  List<FileAsset> findAllByIdIn(List<Long> ids);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select f from FileAsset f where f.id in :ids and f.deletedAt is null")
  List<FileAsset> findAllByIdInAndDeletedAtIsNullForUpdate(@Param("ids") List<Long> ids);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update FileAsset f set f.status = :status where f.id in :ids")
  void updateStatusByIdIn(@Param("ids") List<Long> ids, @Param("status") FileStatus status);
}
