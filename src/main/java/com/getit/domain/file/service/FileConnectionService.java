package com.getit.domain.file.service;

import java.util.List;

public interface FileConnectionService {

  void connectAll(List<Long> fileIds);

  void disconnectAll(List<Long> fileIds);
}
