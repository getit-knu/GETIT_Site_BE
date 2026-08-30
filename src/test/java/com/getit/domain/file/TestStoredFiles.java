package com.getit.domain.file;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import org.springframework.mock.web.MockMultipartFile;

/**
 * 저장소에 <b>실물까지 올려 둔</b> {@link FileAsset} 을 만든다.
 *
 * <p>{@code FileConnectionService} 는 연결 시점에 저장소를 조회해서 실물이 있는지, 신고한
 * 크기를 넘지 않는지 확인한다. 직접 업로드에서는 클라이언트가 작게 신고하고 크게 올리거나
 * 아예 올리지 않을 수 있기 때문이다.
 *
 * <p>그래서 DB 행만 만들고 연결하면 {@code FILE_NOT_UPLOADED} 로 막힌다. 연결까지 가는
 * 테스트는 이 헬퍼를 쓴다. 실제 요청 흐름에서도 업로드가 먼저이므로, 이쪽이 더 정직하다.
 *
 * <p>내용은 신고한 크기와 같은 길이의 빈 바이트다. int 범위를 넘는 크기를 넘기면
 * {@code Math.toIntExact} 가 바로 터진다 — 테스트에서 그런 값을 쓸 일은 없다.
 *
 * <p>같은 길이여야 한다는 점이 중요하다. 크기를 그대로 맞춰야
 * 연결 과정의 크기 동기화가 테스트의 기대값을 바꾸지 않는다.
 */
public final class TestStoredFiles {

  private TestStoredFiles() {
  }

  public static FileAsset stored(
      FileAssetRepository fileAssetRepository,
      FileStorage fileStorage,
      String key,
      String originalName,
      String url,
      long size,
      String contentType,
      Long uploaderId) {
    fileStorage.upload(
        new MockMultipartFile("file", originalName, contentType, new byte[Math.toIntExact(size)]), key);
    return fileAssetRepository.save(
        FileAsset.upload(key, originalName, url, size, contentType, uploaderId));
  }
}
