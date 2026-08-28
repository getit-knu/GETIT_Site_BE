package com.getit.domain.file.storage;

import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

  String upload(MultipartFile file, String key);

  void delete(String key);

  /**
   * 클라이언트가 저장소로 직접 올릴 주소를 발급한다. (명세 13.1)
   *
   * <p>직접 업로드를 지원하지 않는 구현은 비어 있는 값을 준다. 그때는 명세 13.2 의
   * multipart 경로를 쓴다. 로컬 개발이 여기 해당한다.
   */
  default Optional<UploadTicket> issueUploadTicket(String key, String contentType) {
    return Optional.empty();
  }

  /**
   * 파일을 읽을 수 있는 주소.
   *
   * <p>비공개 저장소는 짧게 사는 서명 주소를 준다. 저장된 고정 주소를 그대로 주면
   * 권한 없는 사람도 읽게 되므로, 요청 시점마다 새로 발급한다.
   */
  String downloadUrl(String key);
}
