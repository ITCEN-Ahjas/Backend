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

    /*
     * 기존 DB에 상세 정보가 있고 source_updated_at도 있는 데이터를 대상으로
     * 자동 갱신 기준 시각을 초기화한다.
     */
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

    /*
     * 대표 이미지 또는 상세 이미지 목록이 존재하는 콘텐츠를
     * 이미지 동기화 완료 상태로 설정한다.
     */
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

    /*
     * 대표 이미지와 상세 이미지가 모두 없는 콘텐츠를
     * 이미지 확인 완료 상태로 정리하기 전의 초기화용 쿼리다.
     */
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

    /*
     * 상세 데이터 또는 상세 처리 기준 시각이 존재하는 콘텐츠 수다.
     */
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

    /*
     * 실제 이미지 파일 URL이 존재하는 콘텐츠 수다.
     */
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

    /*
     * 대표 이미지와 상세 이미지 목록이 모두 없는 콘텐츠 수다.
     * 원본에 이미지가 없는 정상 콘텐츠도 포함된다.
     */
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