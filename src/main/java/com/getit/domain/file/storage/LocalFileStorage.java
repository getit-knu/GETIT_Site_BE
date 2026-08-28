package com.getit.domain.file.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * 로컬 디스크 저장소. Azure 를 켜지 않은 환경(로컬 개발·테스트)에서 쓴다.
 *
 * <p>직접 업로드는 지원하지 않는다. {@code issueUploadTicket} 이 비어 있으므로
 * 클라이언트는 명세 13.2 의 multipart 경로를 쓴다.
 */
@Component
@ConditionalOnProperty(
    prefix = "getit.file.azure", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalFileStorage implements FileStorage {

  private final Path rootDir;
  private final String baseUrl;

  public LocalFileStorage(
      @Value("${getit.file.local.path}")
      String rootPath,
      @Value("${getit.file.local.base-url}")
      String baseUrl
  ) {
    this.rootDir = Path.of(rootPath).toAbsolutePath().normalize();
    this.baseUrl = baseUrl;
  }

  @Override
  public String upload(MultipartFile file, String key) {
    Path target = resolve(key);
    try {
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException e) {
      throw new UncheckedIOException("파일 업로드 실패: key=" + key, e);
    }
    return baseUrl + "/" + key;
  }

  @Override
  public void delete(String key) {
    try {
      Files.deleteIfExists(resolve(key));
    } catch (IOException e) {
      throw new UncheckedIOException("파일 삭제 실패: key=" + key, e);
    }
  }

  /** 로컬은 정적 서빙 경로가 곧 읽기 주소다. 서명이 필요 없다. */
  @Override
  public String downloadUrl(String key) {
    return baseUrl + "/" + key;
  }

  private Path resolve(String key) {
    Path target = rootDir.resolve(key).normalize();
    if (!target.startsWith(rootDir)) {
      throw new IllegalArgumentException("잘못된 파일 키입니다: key=" + key);
    }
    return target;
  }
}
