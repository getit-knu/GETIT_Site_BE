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
   * 파일을 읽을 수 있는 주소와 그 유효 시간.
   *
   * <p>비공개 저장소는 짧게 사는 서명 주소를 준다. 저장된 고정 주소를 그대로 주면
   * 권한 없는 사람도 읽게 되므로, 요청 시점마다 새로 발급한다.
   *
   * <p>만료 시간을 함께 돌려준다. 설정에서 따로 읽으면 만료가 없는 구현과 값이 어긋난다.
   */
  SignedUrl downloadUrl(String key);

  /**
   * 저장소에 실제로 올라온 파일의 크기와 형식. 없으면 비어 있다.
   *
   * <p>직접 업로드는 클라이언트가 신고한 값으로 주소를 발급한다. 신고값과 실제가 다를 수
   * 있으므로, 리소스에 연결하기 전에 여기서 실물을 확인한다.
   */
  default Optional<StoredObject> describe(String key) {
    return Optional.empty();
  }
}
