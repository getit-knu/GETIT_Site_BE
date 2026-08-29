package com.getit.domain.file.storage;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import com.getit.domain.file.entity.StorageVisibility;

/**
 * 저장 키의 접두어를 보고 공개·비공개 저장소를 고른다.
 *
 * <p>{@code downloadUrl(key)} 처럼 용도를 모르는 채 불리는 자리가 있어서, 키 자체가
 * 어디에 있는지 말해줘야 한다. 그래서 키를 {@code public/...} · {@code private/...} 로 만든다.
 *
 * <p>둘을 다른 스토리지 계정에 두는 이유는 보존 정책이 정반대이기 때문이다.
 * 백업은 오래되면 지워야 하고 업로드 파일은 절대 자동 삭제되면 안 된다.
 * 수명 주기 정책은 계정 단위라, 한 계정에 섞으면 규칙 하나를 잘못 적어
 * 과제 제출물이 사라질 수 있다.
 */
@RequiredArgsConstructor
public class RoutingFileStorage implements FileStorage {

  private static final String PUBLIC_PREFIX = StorageVisibility.PUBLIC.getKeyPrefix() + "/";

  private final FileStorage publicStorage;
  private final FileStorage privateStorage;

  private FileStorage route(String key) {
    return key.startsWith(PUBLIC_PREFIX) ? publicStorage : privateStorage;
  }

  @Override
  public String upload(MultipartFile file, String key) {
    return route(key).upload(file, key);
  }

  @Override
  public void delete(String key) {
    route(key).delete(key);
  }

  @Override
  public Optional<UploadTicket> issueUploadTicket(String key, String contentType) {
    return route(key).issueUploadTicket(key, contentType);
  }

  @Override
  public SignedUrl downloadUrl(String key) {
    return route(key).downloadUrl(key);
  }

  @Override
  public Optional<StoredObject> describe(String key) {
    return route(key).describe(key);
  }
}
