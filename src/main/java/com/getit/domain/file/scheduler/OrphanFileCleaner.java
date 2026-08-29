package com.getit.domain.file.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;

/**
 * 어디에도 연결되지 않은 업로드 파일 정리. (명세 부록 B)
 *
 * <p>업로드 주소를 발급할 때마다 {@code PENDING} 레코드가 생긴다. 사용자가 파일을 고르다
 * 그만두거나, 올린 뒤 저장을 누르지 않으면 그 레코드와 blob 이 그대로 남는다.
 * 정리하지 않으면 DB 행과 저장소 용량이 계속 쌓인다(PR #126 Copilot 리뷰 지적).
 *
 * <p>업로드 주소 유효 시간보다 넉넉히 기다린다. 발급 직후 지우면 올리는 중인 파일이 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleaner {

  /** 명세 부록 B 기준. 이 시간이 지나도 연결되지 않았으면 버려진 것으로 본다. */
  private static final int ORPHAN_HOURS = 24;

  private final FileAssetRepository fileAssetRepository;
  private final FileStorage fileStorage;

  @Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
  @Transactional
  public void clean() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(ORPHAN_HOURS);
    List<FileAsset> orphans =
        fileAssetRepository.findAllByStatusAndCreatedAtBeforeAndDeletedAtIsNull(
            FileStatus.PENDING, threshold);

    if (orphans.isEmpty()) {
      return;
    }

    int removed = 0;
    for (FileAsset orphan : orphans) {
      try {
        fileStorage.delete(orphan.getStoredKey());
        orphan.delete();
        removed++;
      } catch (RuntimeException e) {
        // 하나가 실패해도 나머지는 정리한다. 다음 실행에서 다시 시도된다.
        log.warn("미연결 파일 정리 실패. fileId={} key={}", orphan.getId(), orphan.getStoredKey(), e);
      }
    }
    log.info("미연결 업로드 파일 {}건 정리 (대상 {}건)", removed, orphans.size());
  }
}
