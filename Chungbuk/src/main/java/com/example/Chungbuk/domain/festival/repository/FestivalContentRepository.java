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

    List<FestivalContent> findAllByContentIdIn(Collection<String> contentIds);

    long countByActiveTrue();

    long countByContentTypeIdAndActiveTrue(String contentTypeId);

    @Query("""
            select count(content)
            from FestivalContent content
            where content.active = true
              and (
                    (content.description is not null and content.description <> '')
                 or (content.overview is not null and content.overview <> '')
                 or (content.mainInfoJson is not null and content.mainInfoJson <> '')
              )
            """)
    long countDetailSyncedContents();

    @Query("""
            select max(content.lastSyncedAt)
            from FestivalContent content
            where content.active = true
            """)
    LocalDateTime findLatestSyncedAt();

    @Query("""
            select content.contentTypeId, count(content)
            from FestivalContent content
            where content.active = true
            group by content.contentTypeId
            order by content.contentTypeId
            """)
    List<Object[]> countActiveContentsByContentTypeId();

    @Query("""
            select content.category, count(content)
            from FestivalContent content
            where content.active = true
            group by content.category
            order by content.category
            """)
    List<Object[]> countActiveContentsByCategory();
}