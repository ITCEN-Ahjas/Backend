package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FestivalContentRepository extends
        JpaRepository<FestivalContent, Long>,
        JpaSpecificationExecutor<FestivalContent> {

    Optional<FestivalContent> findByContentId(String contentId);

    Optional<FestivalContent> findByContentIdAndActiveTrue(String contentId);

    boolean existsByContentId(String contentId);

    long countByActiveTrue();

    long countByContentTypeIdAndActiveTrue(String contentTypeId);

    List<FestivalContent> findAllByContentIdIn(Collection<String> contentIds);

    List<FestivalContent> findAllByActiveTrueAndContentTypeIdIn(Collection<String> contentTypeIds);

    @Query("""
            select count(f)
            from FestivalContent f
            where f.active = true
              and (
                    coalesce(f.overview, '') <> ''
                    or coalesce(f.description, '') <> ''
                    or (
                        f.mainInfoJson is not null
                        and f.mainInfoJson <> ''
                        and f.mainInfoJson <> '[]'
                    )
              )
            """)
    long countDetailSyncedContents();

    @Query("""
            select max(f.lastSyncedAt)
            from FestivalContent f
            """)
    LocalDateTime findLatestSyncedAt();

    @Query("""
            select coalesce(f.contentTypeId, 'UNKNOWN'), count(f)
            from FestivalContent f
            where f.active = true
            group by coalesce(f.contentTypeId, 'UNKNOWN')
            order by coalesce(f.contentTypeId, 'UNKNOWN')
            """)
    List<Object[]> countActiveContentsByContentTypeId();

    @Query("""
            select coalesce(f.category, 'UNKNOWN'), count(f)
            from FestivalContent f
            where f.active = true
            group by coalesce(f.category, 'UNKNOWN')
            order by coalesce(f.category, 'UNKNOWN')
            """)
    List<Object[]> countActiveContentsByCategory();
}