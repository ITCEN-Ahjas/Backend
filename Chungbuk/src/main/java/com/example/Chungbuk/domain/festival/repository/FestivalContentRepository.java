package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FestivalContentRepository extends
        JpaRepository<FestivalContent, Long>,
        JpaSpecificationExecutor<FestivalContent> {

    Optional<FestivalContent> findByContentId(String contentId);

    Optional<FestivalContent> findByContentIdAndActiveTrue(String contentId);

    boolean existsByContentId(String contentId);

    List<FestivalContent> findAllByContentIdIn(Collection<String> contentIds);

    List<FestivalContent> findAllByActiveTrueAndContentTypeIdIn(
            Collection<String> contentTypeIds
    );

    long countByActiveTrue();

    long countByContentTypeIdAndActiveTrue(String contentTypeId);

    long countByActiveTrueAndDetailSourceUpdatedAtIsNotNull();

    long countByActiveTrueAndImageSyncCompletedTrue();

    long countByActiveTrueAndImageSyncCompletedFalse();

    long countByActiveTrueAndImageSyncCompletedIsNull();

    long countByActiveTrueAndNextDetailRetryAtIsNotNull();

    long countByActiveTrueAndLastDetailFailureReasonIsNotNull();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE festival_contents
                    SET detail_source_updated_at = source_updated_at,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE is_active = true
                      AND detail_source_updated_at IS NULL
                      AND source_updated_at IS NOT NULL
                      AND (
                            TRIM(COALESCE(overview, '')) <> ''
                            OR TRIM(COALESCE(homepage, '')) <> ''
                            OR TRIM(COALESCE(description, '')) <> ''
                            OR (
                                TRIM(COALESCE(description_source, '')) <> ''
                                AND description_source <> 'DETAIL_COMMON_EMPTY'
                            )
                            OR (
                                TRIM(COALESCE(main_info_json, '')) <> ''
                                AND TRIM(main_info_json) <> '[]'
                            )
                      )
                    """,
            nativeQuery = true
    )
    int initializeLegacyDetailSourceUpdatedAt();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE festival_contents
                    SET image_sync_completed = true,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE is_active = true
                      AND (
                            image_sync_completed IS NULL
                            OR image_sync_completed = false
                      )
                      AND (
                            TRIM(COALESCE(image_url, '')) <> ''
                            OR (
                                TRIM(COALESCE(image_urls_json, '')) <> ''
                                AND TRIM(image_urls_json) <> '[]'
                            )
                      )
                    """,
            nativeQuery = true
    )
    int initializeImageSyncCompletedTrue();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            value = """
                    UPDATE festival_contents
                    SET image_sync_completed = false,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE is_active = true
                      AND (
                            image_sync_completed IS NULL
                            OR image_sync_completed = true
                      )
                      AND (
                            image_url IS NULL
                            OR TRIM(image_url) = ''
                      )
                      AND (
                            image_urls_json IS NULL
                            OR TRIM(image_urls_json) = ''
                            OR TRIM(image_urls_json) = '[]'
                      )
                    """,
            nativeQuery = true
    )
    int initializeImageSyncCompletedFalse();

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM festival_contents
                    WHERE is_active = true
                      AND (
                            TRIM(COALESCE(overview, '')) <> ''
                            OR TRIM(COALESCE(homepage, '')) <> ''
                            OR TRIM(COALESCE(description, '')) <> ''
                            OR (
                                TRIM(COALESCE(description_source, '')) <> ''
                                AND description_source <> 'DETAIL_COMMON_EMPTY'
                            )
                            OR (
                                TRIM(COALESCE(main_info_json, '')) <> ''
                                AND TRIM(main_info_json) <> '[]'
                            )
                            OR detail_source_updated_at IS NOT NULL
                      )
                    """,
            nativeQuery = true
    )
    long countDetailSyncedContents();

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM festival_contents
                    WHERE is_active = true
                      AND (
                            TRIM(COALESCE(image_url, '')) <> ''
                            OR (
                                TRIM(COALESCE(image_urls_json, '')) <> ''
                                AND TRIM(image_urls_json) <> '[]'
                            )
                      )
                    """,
            nativeQuery = true
    )
    long countActiveContentsWithImages();

    @Query(
            value = """
                    SELECT COUNT(*)
                    FROM festival_contents
                    WHERE is_active = true
                      AND (
                            image_url IS NULL
                            OR TRIM(image_url) = ''
                      )
                      AND (
                            image_urls_json IS NULL
                            OR TRIM(image_urls_json) = ''
                            OR TRIM(image_urls_json) = '[]'
                      )
                    """,
            nativeQuery = true
    )
    long countActiveContentsWithoutImages();

    @Query("""
            select max(content.lastSyncedAt)
            from FestivalContent content
            """)
    LocalDateTime findLatestSyncedAt();

    @Query("""
            select content.contentTypeId, count(content)
            from FestivalContent content
            where content.active = true
            group by content.contentTypeId
            """)
    List<Object[]> countActiveContentsByContentTypeId();

    @Query("""
            select content.category, count(content)
            from FestivalContent content
            where content.active = true
            group by content.category
            """)
    List<Object[]> countActiveContentsByCategory();
}