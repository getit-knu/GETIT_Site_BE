package com.getit.domain.file.service;

import com.getit.domain.file.entity.FileAsset;
import com.getit.domain.file.exception.FileErrorCode;
import com.getit.domain.file.repository.FileAssetRepository;
import com.getit.domain.file.storage.FileStorage;
import com.getit.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileQueryServiceImpl implements FileQueryService {

  private final FileAssetRepository fileAssetRepository;
  private final FileStorage fileStorage;

  @Override
  public FileInfo findById(Long fileId) {
    FileAsset file = fileAssetRepository.findByIdAndDeletedAtIsNull(fileId)
        .orElseThrow(() -> new BusinessException(FileErrorCode.FILE_NOT_FOUND));
    return toInfo(file);
  }

  /**
   * 저장된 고정 주소 대신 저장소가 발급한 주소를 채운다.
   *
   * <p>비공개 컨테이너의 고정 주소는 그대로는 열리지 않는다. 이 한 곳에서 바꿔주면
   * {@code FileInfo.url()} 을 쓰는 강의·제출물·운영진 화면이 그대로 동작한다.
   */
  private FileInfo toInfo(FileAsset file) {
    return FileInfo.from(file, fileStorage.downloadUrl(file.getStoredKey()).url());
  }

  @Override
  public List<FileInfo> findAllByIds(List<Long> fileIds) {
    return fileAssetRepository.findAllByIdInAndDeletedAtIsNull(fileIds).stream()
        .map(this::toInfo)
        .toList();
  }
}
