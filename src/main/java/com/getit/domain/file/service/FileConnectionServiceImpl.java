package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.entity.FileStatus;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.repository.FileConnectionView;
import com.getit.domain.file.storage.FileStorage;
import com.getit.domain.file.storage.StoredObject;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FileConnectionServiceImpl implements FileConnectionService {

  private final FileAssetRepository fileAssetRepository;
  private final FileStorage fileStorage;

  /**
   * 저장소 확인을 먼저 하고, 그다음에 락을 잡는다.
   *
   * <p>확인은 파일 하나당 저장소 조회 한 번이다. Azure 라면 네트워크 왕복이라 시간이 든다.
   * 락을 쥔 채로 돌면 같은 파일을 건드리는 다른 요청이 그 시간만큼 밀린다(PR #163 리뷰 지적).
   * 그래서 락 보유 구간에는 DB 작업만 남긴다.
   *
   * <p>확인과 락 사이에 다른 요청이 먼저 연결했을 수 있으므로, 락을 잡은 뒤에 다시 확인한다.
   * 실물은 생성 전용 SAS 라 덮어쓸 수 없으니 그사이 바뀌지 않는다.
   */
  @Override
  public void connectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();

    List<FileConnectionView> candidates =
        fileAssetRepository.findConnectionViewsByIdIn(distinctFileIds);
    validateAllFound(distinctFileIds, candidates.stream().map(FileConnectionView::id).toList());
    validateNoneConnected(candidates.stream()
        .filter(candidate -> candidate.status() == FileStatus.CONNECTED)
        .map(FileConnectionView::id)
        .toList());

    Map<Long, Long> verifiedSizes = candidates.stream()
        .collect(Collectors.toMap(FileConnectionView::id, this::verifiedSize));

    // 엔티티는 여기서 처음 읽는다. 락을 잡은 DB 의 최신 상태다.
    List<FileAsset> files =
        fileAssetRepository.findAllByIdInAndDeletedAtIsNullForUpdate(distinctFileIds);
    validateAllFound(distinctFileIds, files.stream().map(FileAsset::getId).toList());
    validateNoneConnected(files.stream()
        .filter(FileAsset::isInUse)
        .map(FileAsset::getId)
        .toList());

    files.forEach(file -> {
      file.syncSize(verifiedSizes.get(file.getId()));
      file.connect();
    });
  }

  @Override
  public void disconnectAll(List<Long> fileIds) {
    if (fileIds == null || fileIds.isEmpty()) {
      return;
    }
    List<Long> distinctFileIds = fileIds.stream().distinct().toList();
    List<FileAsset> files = fileAssetRepository.findAllByIdInAndDeletedAtIsNull(distinctFileIds);
    validateAllFound(distinctFileIds, files.stream().map(FileAsset::getId).toList());

    files.forEach(FileAsset::disconnect);
  }

  /**
   * 저장소에 실물이 있는지, 신고한 크기를 넘지 않았는지 확인하고 실제 크기를 돌려준다.
   *
   * <p>직접 업로드(명세 13.1)는 클라이언트가 신고한 크기로 주소를 발급한다. SAS 자체에는
   * 크기 제한을 걸 수 없어서, 작게 신고하고 크게 올리거나 아예 올리지 않은 채로 fileId 만
   * 연결할 수 있다(PR #126 Copilot 리뷰 지적). 연결은 되돌리기 어려우므로 여기서 막는다.
   *
   * <p>신고 크기는 이미 용도별 상한을 통과했다. 실물이 신고 크기 이하이면 상한도 지켜진다.
   */
  private long verifiedSize(FileConnectionView file) {
    Optional<StoredObject> stored = fileStorage.describe(file.storedKey());
    if (stored.isEmpty()) {
      log.warn("업로드되지 않은 파일 연결 시도. fileId={} key={}", file.id(), file.storedKey());
      throw new BusinessException(FileErrorCode.FILE_NOT_UPLOADED);
    }
    long actualSize = stored.get().size();
    if (actualSize > file.size()) {
      log.warn("신고 크기 초과. fileId={} 신고={} 실제={}", file.id(), file.size(), actualSize);
      throw new BusinessException(FileErrorCode.FILE_SIZE_MISMATCH);
    }
    return actualSize;
  }

  private void validateAllFound(List<Long> requestedFileIds, List<Long> foundFileIds) {
    if (foundFileIds.size() == requestedFileIds.size()) {
      return;
    }
    Set<Long> foundIds = Set.copyOf(foundFileIds);
    List<Long> missingFileIds =
        requestedFileIds.stream().filter(id -> !foundIds.contains(id)).toList();
    log.warn("존재하지 않는 파일 연결/해제 시도. fileIds={}", missingFileIds);
    throw new BusinessException(FileErrorCode.FILE_NOT_FOUND);
  }

  private void validateNoneConnected(List<Long> alreadyConnectedFileIds) {
    if (alreadyConnectedFileIds.isEmpty()) {
      return;
    }
    log.warn("이미 연결된 파일 재연결 시도. fileIds={}", alreadyConnectedFileIds);
    throw new BusinessException(FileErrorCode.FILE_ALREADY_CONNECTED);
  }
}
