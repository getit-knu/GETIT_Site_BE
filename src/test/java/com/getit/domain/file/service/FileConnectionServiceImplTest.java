package com.getit.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.repository.FileConnectionView;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.StoredObject;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileConnectionServiceImplTest {

  private static final long DECLARED_SIZE = 10L;

  @Mock
  private FileAssetRepository fileAssetRepository;

  @Mock
  private FileStorage fileStorage;

  @InjectMocks
  private FileConnectionServiceImpl fileConnectionService;

  private FileAsset uploadedFile() {
    return FileAsset.upload(
        "key.txt", "original.txt", "http://localhost/x.txt", DECLARED_SIZE, "text/plain", 1L);
  }

  private FileAsset uploadedFile(Long id) {
    FileAsset file = uploadedFile();
    ReflectionTestUtils.setField(file, "id", id);
    return file;
  }

  /** 락을 잡기 전 단계에서 읽히는 값. 기본은 아직 연결되지 않은 정상 파일이다. */
  private FileConnectionView view(Long id) {
    return view(id, "key.txt", FileStatus.PENDING);
  }

  private FileConnectionView view(Long id, String storedKey, FileStatus status) {
    return new FileConnectionView(id, storedKey, DECLARED_SIZE, status);
  }

  private void candidates(List<Long> ids, FileConnectionView... views) {
    when(fileAssetRepository.findConnectionViewsByIdIn(ids)).thenReturn(List.of(views));
  }

  private void locked(List<Long> ids, FileAsset... files) {
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNullForUpdate(ids))
        .thenReturn(List.of(files));
  }

  /** 실물이 신고한 크기 그대로 올라와 있는, 정상적인 저장소 상태. */
  private void storageHasEverything() {
    when(fileStorage.describe(anyString()))
        .thenReturn(Optional.of(new StoredObject(DECLARED_SIZE, "text/plain")));
  }

  @Test
  @DisplayName("connectAll: 조회한 엔티티를 전부 CONNECTED 로 바꾼다")
  void connectsAllFiles() {
    FileAsset file1 = uploadedFile(1L);
    FileAsset file2 = uploadedFile(2L);
    candidates(List.of(1L, 2L), view(1L), view(2L));
    storageHasEverything();
    locked(List.of(1L, 2L), file1, file2);

    fileConnectionService.connectAll(List.of(1L, 2L));

    assertThat(file1.getStatus()).isEqualTo(FileStatus.CONNECTED);
    assertThat(file2.getStatus()).isEqualTo(FileStatus.CONNECTED);
  }

  @Test
  @DisplayName("connectAll: 저장소 확인을 끝낸 뒤에 락을 잡는다")
  void verifiesStorageBeforeTakingTheLock() {
    FileAsset file = uploadedFile(1L);
    candidates(List.of(1L), view(1L));
    storageHasEverything();
    locked(List.of(1L), file);

    fileConnectionService.connectAll(List.of(1L));

    // 저장소 확인은 Azure 라면 네트워크 왕복이다. 락을 쥔 채로 돌면 같은 파일을 건드리는
    // 다른 요청이 그 시간만큼 밀린다 (PR #163 리뷰).
    InOrder order = inOrder(fileStorage, fileAssetRepository);
    order.verify(fileStorage).describe("key.txt");
    order.verify(fileAssetRepository).findAllByIdInAndDeletedAtIsNullForUpdate(List.of(1L));
  }

  @Test
  @DisplayName("connectAll: 이미 연결된 파일이면 저장소를 확인하기 전에 막는다")
  void throwsWhenConnectingAlreadyConnectedFile() {
    candidates(List.of(1L, 2L), view(1L), view(2L, "key.txt", FileStatus.CONNECTED));

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_ALREADY_CONNECTED);
    verify(fileStorage, never()).describe(anyString());
    verify(fileAssetRepository, never()).findAllByIdInAndDeletedAtIsNullForUpdate(anyList());
  }

  @Test
  @DisplayName("connectAll: 확인과 락 사이에 다른 요청이 먼저 연결했으면 막는다")
  void throwsWhenAnotherRequestConnectedWhileVerifying() {
    FileAsset connectedMeanwhile = uploadedFile(1L);
    connectedMeanwhile.connect();
    // 락을 잡기 전에는 아직 PENDING 으로 보였다.
    candidates(List.of(1L), view(1L));
    storageHasEverything();
    locked(List.of(1L), connectedMeanwhile);

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_ALREADY_CONNECTED);
  }

  @Test
  @DisplayName("connectAll: 존재하지 않는 파일이 섞여 있으면 예외 발생")
  void throwsWhenConnectingAllWithMissingFile() {
    candidates(List.of(1L, 2L), view(1L));

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
  }

  @Test
  @DisplayName("connectAll: 실물이 저장소에 없으면 연결하지 않는다")
  void throwsWhenFileWasNeverUploaded() {
    candidates(List.of(1L), view(1L));
    when(fileStorage.describe(anyString())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_UPLOADED);
    verify(fileAssetRepository, never()).findAllByIdInAndDeletedAtIsNullForUpdate(anyList());
  }

  @Test
  @DisplayName("connectAll: 신고한 크기보다 크게 올렸으면 연결하지 않는다")
  void throwsWhenActualSizeExceedsDeclaredSize() {
    candidates(List.of(1L), view(1L));
    when(fileStorage.describe(anyString()))
        .thenReturn(Optional.of(new StoredObject(DECLARED_SIZE + 1, "text/plain")));

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_SIZE_MISMATCH);
    verify(fileAssetRepository, never()).findAllByIdInAndDeletedAtIsNullForUpdate(anyList());
  }

  @Test
  @DisplayName("connectAll: 하나라도 확인에 실패하면 아무것도 연결되지 않는다")
  void connectsNothingWhenAnyFileFailsVerification() {
    FileAsset ok = uploadedFile(1L);
    candidates(List.of(1L, 2L), view(1L), view(2L, "missing.txt", FileStatus.PENDING));
    when(fileStorage.describe("key.txt"))
        .thenReturn(Optional.of(new StoredObject(DECLARED_SIZE, "text/plain")));
    when(fileStorage.describe("missing.txt")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> fileConnectionService.connectAll(List.of(1L, 2L)))
        .isInstanceOf(BusinessException.class)
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_UPLOADED);

    // 확인을 전부 끝낸 뒤에 상태를 바꾸므로, 통과한 파일도 PENDING 으로 남는다.
    assertThat(ok.getStatus()).isEqualTo(FileStatus.PENDING);
  }

  @Test
  @DisplayName("connectAll: 실제 크기가 신고보다 작으면 실제 크기로 맞춘다")
  void syncsSizeDownToActual() {
    FileAsset file = uploadedFile(1L);
    candidates(List.of(1L), view(1L));
    when(fileStorage.describe(anyString()))
        .thenReturn(Optional.of(new StoredObject(4L, "text/plain")));
    locked(List.of(1L), file);

    fileConnectionService.connectAll(List.of(1L));

    // 엔티티를 직접 바꾸므로 더티 체킹으로 저장된다. 벌크 UPDATE 시절에는
    // 영속성 컨텍스트가 비워져 이 값이 유실됐다 (이슈 #160).
    assertThat(file.getSize()).isEqualTo(4L);
    assertThat(file.getStatus()).isEqualTo(FileStatus.CONNECTED);
  }

  @Test
  @DisplayName("disconnectAll: 조회한 엔티티를 전부 PENDING 으로 바꾼다")
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
  @DisplayName("disconnectAll: 해제할 때는 저장소를 확인하지 않는다")
  void doesNotTouchStorageWhenDisconnecting() {
    FileAsset file = uploadedFile(1L);
    file.connect();
    when(fileAssetRepository.findAllByIdInAndDeletedAtIsNull(List.of(1L)))
        .thenReturn(List.of(file));

    fileConnectionService.disconnectAll(List.of(1L));

    verify(fileStorage, never()).describe(anyString());
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
        .hasFieldOrPropertyWithValue("errorCode", FileErrorCode.FILE_NOT_FOUND);
    assertThat(file1.getStatus()).isEqualTo(FileStatus.CONNECTED);
  }
}
