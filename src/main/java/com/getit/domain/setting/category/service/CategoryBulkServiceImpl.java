package com.getit.domain.setting.category.service;

import com.getit.domain.lecture.service.CategoryLectureLinkService;
import com.getit.domain.setting.category.entity.SubCategory;
import com.getit.domain.setting.category.entity.Track;
import com.getit.domain.setting.category.exception.CategoryErrorCode;
import com.getit.domain.setting.category.repository.SubCategoryRepository;
import com.getit.domain.setting.category.repository.TrackRepository;
import com.getit.domain.setting.category.service.TrackUpsert.SubCategoryNode;
import com.getit.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryBulkServiceImpl implements CategoryBulkService {

  private final TrackRepository trackRepository;
  private final SubCategoryRepository subCategoryRepository;
  private final CategoryLectureLinkService categoryLectureLinkService;

  @Override
  public void replaceTree(List<TrackUpsert> desired, boolean force) {
    Map<Long, Track> trackById = trackRepository.findAllForUpdate().stream()
        .collect(Collectors.toMap(Track::getId, Function.identity()));
    List<SubCategory> currentSubs = subCategoryRepository.findAllForUpdate();
    Map<Long, SubCategory> subById = currentSubs.stream()
        .collect(Collectors.toMap(SubCategory::getId, Function.identity()));

    Set<Long> keepTrackIds = idsOf(desired.stream().map(TrackUpsert::id));
    Set<Long> keepSubIds = idsOf(desired.stream()
        .flatMap(track -> track.subCategories().stream())
        .map(SubCategoryNode::id));

    List<Track> tracksToDelete = trackById.values().stream()
        .filter(track -> !keepTrackIds.contains(track.getId()))
        .toList();
    List<SubCategory> subsToDelete = currentSubs.stream()
        .filter(sub -> !keepSubIds.contains(sub.getId()))
        .toList();

    disconnectOrReject(tracksToDelete, subsToDelete, force);
    subCategoryRepository.deleteAll(subsToDelete);
    trackRepository.deleteAll(tracksToDelete);

    for (int i = 0; i < desired.size(); i++) {
      TrackUpsert desiredTrack = desired.get(i);
      Track track = desiredTrack.id() == null
          ? trackRepository.save(Track.create(desiredTrack.name(), i + 1))
          : requireTrack(trackById, desiredTrack.id(), desiredTrack.name(), i + 1);
      upsertSubCategories(track.getId(), desiredTrack.subCategories(), subById);
    }
  }

  private void upsertSubCategories(Long trackId, List<SubCategoryNode> desired, Map<Long, SubCategory> subById) {
    for (int j = 0; j < desired.size(); j++) {
      SubCategoryNode node = desired.get(j);
      if (node.id() == null) {
        subCategoryRepository.save(SubCategory.create(node.name(), j + 1, trackId));
        continue;
      }
      SubCategory subCategory = subById.get(node.id());
      if (subCategory == null || !subCategory.getTrackId().equals(trackId)) {
        throw new BusinessException(CategoryErrorCode.SUBCATEGORY_NOT_FOUND);
      }
      subCategory.update(node.name(), j + 1);
    }
  }

  private void disconnectOrReject(List<Track> tracksToDelete, List<SubCategory> subsToDelete, boolean force) {
    List<Long> trackIds = tracksToDelete.stream().map(Track::getId).toList();
    List<Long> subIds = subsToDelete.stream().map(SubCategory::getId).toList();
    if (force) {
      if (!subIds.isEmpty()) {
        categoryLectureLinkService.disconnectLecturesBySubCategoryIds(subIds);
      }
      trackIds.forEach(categoryLectureLinkService::disconnectLecturesByTrackId);
      return;
    }
    boolean trackInUse = !trackIds.isEmpty()
        && categoryLectureLinkService.countLecturesByTrackIds(trackIds).values().stream().anyMatch(count -> count > 0);
    boolean subInUse = !subIds.isEmpty()
        && categoryLectureLinkService.countLecturesBySubCategoryIds(subIds).values().stream().anyMatch(count -> count > 0);
    if (trackInUse || subInUse) {
      throw new BusinessException(CategoryErrorCode.CATEGORY_IN_USE);
    }
  }

  private Track requireTrack(Map<Long, Track> trackById, Long id, String name, int order) {
    Track track = trackById.get(id);
    if (track == null) {
      throw new BusinessException(CategoryErrorCode.TRACK_NOT_FOUND);
    }
    track.update(name, order);
    return track;
  }

  private Set<Long> idsOf(Stream<Long> ids) {
    return ids.filter(Objects::nonNull).collect(Collectors.toSet());
  }
}
