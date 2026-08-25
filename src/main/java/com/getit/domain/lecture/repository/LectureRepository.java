package com.getit.domain.lecture.repository;

import com.getit.domain.lecture.entity.Lecture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

  Optional<Lecture> findByIdAndDeletedAtIsNull(Long id);

  @Query("""
      select l from Lecture l
      where l.generationId = :generationId
        and l.deletedAt is null
        and (:trackId is null or l.trackId = :trackId)
        and (:subCategoryId is null or l.subCategoryId = :subCategoryId)
      order by l.week asc, l.id asc
      """)
  List<Lecture> findAllByFilters(
      @Param("generationId") Long generationId,
      @Param("trackId") Long trackId,
      @Param("subCategoryId") Long subCategoryId
  );

  long countByTrackIdAndDeletedAtIsNull(Long trackId);

  long countBySubCategoryIdAndDeletedAtIsNull(Long subCategoryId);

  @Query("""
      select l.subCategoryId as subCategoryId, count(l) as count
      from Lecture l
      where l.subCategoryId in :subCategoryIds and l.deletedAt is null
      group by l.subCategoryId
      """)
  List<SubCategoryLectureCount> countBySubCategoryIdsGrouped(@Param("subCategoryIds") List<Long> subCategoryIds);

  @Modifying(clearAutomatically = true)
  @Query("update Lecture l set l.subCategoryId = null where l.subCategoryId in :subCategoryIds")
  void disconnectBySubCategoryIds(@Param("subCategoryIds") List<Long> subCategoryIds);

  @Modifying(clearAutomatically = true)
  @Query("update Lecture l set l.trackId = null, l.subCategoryId = null where l.trackId = :trackId")
  void disconnectByTrackId(@Param("trackId") Long trackId);

  interface SubCategoryLectureCount {
    Long getSubCategoryId();
    Long getCount();
  }
}
