package com.getit.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.getit.domain.file.dto.PresignedUploadRequest;
import com.getit.domain.file.dto.PresignedUploadResponse;
import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FilePurpose;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.UploadTicket;
import com.getit.global.exception.BusinessException;

/**
 * 직접 업로드 주소 발급. (명세 13.1)
 *
 * <p>검증이 발급 <b>전에</b> 끝나는지가 핵심이다. 올린 뒤에 거절하면 저장소에는 이미
 * 파일이 들어가 있고, 아무도 참조하지 않는 채로 남는다.
 */
@ExtendWith(MockitoExtension.class)
class FileDirectUploadTest {

  private static final long UPLOADER_ID = 7L;

  @Mock
  private FileStorage fileStorage;

  @Mock
  private FileAssetRepository fileAssetRepository;

  @InjectMocks
  private FileService fileService;

  private static PresignedUploadRequest request(String fileName, long size, FilePurpose purpose) {
    return new PresignedUploadRequest(fileName, "application/pdf", size, purpose);
  }

  private static UploadTicket ticket() {
    return new UploadTicket(
        "https://acct.blob.core.windows.net/uploads/k.pdf?sig=x",
        "https://acct.blob.core.windows.net/uploads/k.pdf",
        "PUT",
        Map.of("x-ms-blob-type", "BlockBlob"),
        600);
  }

  @Test
  @DisplayName("업로드 주소와 함께 연결에 쓸 fileId 를 내려준다")
  void issuesUploadUrlWithFileId() {
    given(fileStorage.issueUploadTicket(anyString(), anyString())).willReturn(Optional.of(ticket()));
    FileAsset saved = FileAsset.upload("k.pdf", "자료.pdf", "url", 100L, "application/pdf", UPLOADER_ID);
    ReflectionTestUtils.setField(saved, "id", 501L);
    given(fileAssetRepository.save(any())).willReturn(saved);

    PresignedUploadResponse response =
        fileService.issueUploadUrl(request("자료.pdf", 100L, FilePurpose.LECTURE_MATERIAL), UPLOADER_ID);

    assertThat(response.fileId()).isEqualTo(501L);
    assertThat(response.method()).isEqualTo("PUT");
    assertThat(response.uploadUrl()).contains("sig=");
    // 이 헤더가 빠지면 저장소가 400 으로 거부한다. 프론트가 반드시 실어야 한다.
    assertThat(response.headers()).containsEntry("x-ms-blob-type", "BlockBlob");
    assertThat(response.expiresIn()).isPositive();
  }

  @Test
  @DisplayName("허용되지 않은 확장자는 주소를 발급하기 전에 거절한다")
  void rejectsDisallowedExtensionBeforeIssuing() {
    assertThatThrownBy(() ->
        fileService.issueUploadUrl(request("악성.exe", 100L, FilePurpose.LECTURE_MATERIAL), UPLOADER_ID))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("용량 제한을 넘으면 주소를 발급하기 전에 거절한다")
  void rejectsOversizedFileBeforeIssuing() {
    long tooBig = FilePurpose.PROFILE_IMAGE.getMaxSizeBytes() + 1;

    assertThatThrownBy(() -> fileService.issueUploadUrl(
        new PresignedUploadRequest("사진.png", "image/png", tooBig, FilePurpose.PROFILE_IMAGE),
        UPLOADER_ID))
        .isInstanceOf(BusinessException.class);
  }

  @Test
  @DisplayName("직접 업로드를 지원하지 않는 저장소면 명확히 알린다")
  void failsClearlyWhenStorageCannotIssueTicket() {
    // 로컬 개발 환경이 여기 해당한다. 조용히 실패하면 프론트가 원인을 알 수 없다.
    given(fileStorage.issueUploadTicket(anyString(), anyString())).willReturn(Optional.empty());

    assertThatThrownBy(() ->
        fileService.issueUploadUrl(request("자료.pdf", 100L, FilePurpose.LECTURE_MATERIAL), UPLOADER_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("multipart");
  }
}
