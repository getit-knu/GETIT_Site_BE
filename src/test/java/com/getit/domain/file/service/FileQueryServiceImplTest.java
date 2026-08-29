package com.getit.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.SignedUrl;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileQueryServiceImplTest {

  @Mock
  private FileAssetRepository fileAssetRepository;

  // 저장된 고정 주소가 아니라 저장소가 발급한 주소를 쓴다. 비공개 컨테이너의
  // 고정 주소는 그대로는 열리지 않기 때문이다.
  @Mock
  private FileStorage fileStorage;

  @InjectMocks
  private FileQueryServiceImpl fileQueryService;

  private FileAsset uploadedFile() {
    return FileAsset.upload("key.txt", "original.txt", "http://localhost/x.txt", 10L, "text/plain", 1L);
  }

  @Test
  @DisplayName("존재하는 파일: FileInfo 반환")
  void returnsFileInfo() {
    FileAsset file = uploadedFile();
    when(fileAssetRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(file));
    when(fileStorage.downloadUrl("key.txt"))
        .thenReturn(new SignedUrl("https://signed/key.txt?sig=1", 300));

    FileInfo info = fileQueryService.findById(1L);

    assertThat(info.url()).isEqualTo("https://signed/key.txt?sig=1");
    // 만료를 버리면 소비자가 상수나 설정값을 따로 읽게 되고, 실제 서명 만료와 어긋난다.
    assertThat(info.urlExpiresInSeconds()).isEqualTo(300);
    assertThat(info.originalName()).isEqualTo("original.txt");
    assertThat(info.size()).isEqualTo(10L);
  }

  @Test
  @DisplayName("존재하지 않는 파일: 예외 발생")
  void throwsWhenNotFound() {
    when(fileAssetRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileQueryService.findById(1L))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
  }

  @Test
  @DisplayName("여러 fileId: 한 번에 조회")
  void findsAllByIds() {
    FileAsset file = uploadedFile();
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L))).thenReturn(List.of(file));
    when(fileStorage.downloadUrl("key.txt")).thenReturn(new SignedUrl("https://signed/key.txt?sig=1", 300));

    List<FileInfo> infos = fileQueryService.findAllByIds(List.of(1L));

    assertThat(infos).hasSize(1);
    assertThat(infos.get(0).originalName()).isEqualTo("original.txt");
    assertThat(infos.get(0).url()).isEqualTo("https://signed/key.txt?sig=1");
  }
}
