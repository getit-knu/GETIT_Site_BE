package com.getit.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileConnectionServiceImplTest {

  @Mock
  private FileAssetRepository fileAssetRepository;

  @InjectMocks
  private FileConnectionServiceImpl fileConnectionService;

  private FileAsset uploadedFile() {
    return FileAsset.upload("key.txt", "original.txt", "http://localhost/x.txt", 10L, "text/plain", 1L);
  }

  private FileAsset uploadedFile(Long id) {
    FileAsset file = uploadedFile();
    ReflectionTestUtils.setField(file, "id", id);
    return file;
  }

  @Test
  @DisplayName("connectAll: 전부 CONNECTED로 전이")
  void connectsAllFiles() {
    FileAsset file1 = uploadedFile(1L);
    FileAsset file2 = uploadedFile(2L);
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .thenReturn(List.of(file1, file2));

    fileConnectionService.connectAll(List.of(1L, 2L));

    assertThat(file1.getStatus()).isEqualTo(FileStatus.CONNECTED);
    assertThat(file2.getStatus()).isEqualTo(FileStatus.CONNECTED);
  }

  @Test
  @DisplayName("connectAll: 이미 연결된 파일이 섞여 있으면 예외 발생")
  void throwsWhenConnectingAlreadyConnectedFile() {
    FileAsset file1 = uploadedFile(1L);
    FileAsset alreadyConnected = uploadedFile(2L);
    alreadyConnected.connect();
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .thenReturn(List.of(file1, alreadyConnected));

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_ALREADY_CONNECTED)
        .hasMessageContaining("2");
  }

  @Test
  @DisplayName("connectAll: 존재하지 않는 파일이 섞여 있으면 예외 발생")
  void throwsWhenConnectingAllWithMissingFile() {
    FileAsset file1 = uploadedFile(1L);
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .thenReturn(List.of(file1));

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND)
        .hasMessageContaining("2");
  }

  @Test
  @DisplayName("disconnectAll: 전부 PENDING으로 전이")
  void disconnectsAllFiles() {
    FileAsset file1 = uploadedFile(1L);
    file1.connect();
    FileAsset file2 = uploadedFile(2L);
    file2.connect();
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .thenReturn(List.of(file1, file2));

    fileConnectionService.disconnectAll(List.of(1L, 2L));

    assertThat(file1.getStatus()).isEqualTo(FileStatus.PENDING);
    assertThat(file2.getStatus()).isEqualTo(FileStatus.PENDING);
  }

  @Test
  @DisplayName("disconnectAll: 존재하지 않는 파일이 섞여 있으면 예외 발생")
  void throwsWhenDisconnectingAllWithMissingFile() {
    FileAsset file1 = uploadedFile(1L);
    file1.connect();
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L, 2L)))
        .thenReturn(List.of(file1));

    assertThatThrownBy(() -> fileConnectionService.disconnectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND)
        .hasMessageContaining("2");
  }
}
