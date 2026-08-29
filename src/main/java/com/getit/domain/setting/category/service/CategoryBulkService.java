package com.getit.domain.setting.category.service;

import java.util.List;

public interface CategoryBulkService {

  void replaceTree(List<TrackUpsert> desired, boolean force);
}
