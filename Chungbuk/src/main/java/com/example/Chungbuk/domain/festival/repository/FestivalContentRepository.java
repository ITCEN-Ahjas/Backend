package com.example.Chungbuk.domain.festival.repository;

import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface FestivalContentRepository extends
        JpaRepository<FestivalContent, Long>,
        JpaSpecificationExecutor<FestivalContent> {

    Optional<FestivalContent> findByContentId(String contentId);

    Optional<FestivalContent> findByContentIdAndActiveTrue(String contentId);

    boolean existsByContentId(String contentId);

    List<FestivalContent> findAllByContentIdIn(
            Collection<String> contentIds
    );

    List<FestivalContent> findAllByActiveTrueAndContentTypeIdIn(
            Collection<String> contentTypeIds
    );

    List<FestivalContent>
    findAllByActiveTrueAndContentTypeIdInOrderByContentTypeIdAscIdAsc(
            Collection<String> contentTypeIds
    );

    long countByActiveTrue();

    long countByContentTypeIdAndActiveTrue(
            String contentTypeId
    );

    long countByActiveTrueAndDetailSourceUpdatedAtIsNotNull();

    long countByActiveTrueAndImageSyncCompletedTrue();

    long countByActiveTrueAndImageSyncCompletedFalse();

    /*
     * 기존 DB에 상세 정보가 이미 저장되어 있고,
     * TourAPI 목록 응답의 modifiedtime도 보유한 데이터만 대상으로 한다.
     *
     * 외부 API를 호출하지 않고 detail_source_updated_at 기준값을 채운다.
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
     * 대표 이미지 또는 상세 이미지 목록이 실제로 저장된 콘텐츠는
     * 이미지 동기화 완료 상태로 맞춘다.
     *
     * 이전 refresh 실행에서 이미지 유무와 관계없이 true가 들어간 경우도
     * 이 쿼리와 아래 false 보정 쿼리로 정리된다.
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
     * 대표 이미지와 상세 이미지 목록이 모두 없는 콘텐츠는
     * 이미지 확인 필요 상태로 맞춘다.
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
     * JPQL에서 TEXT/CLOB 컬럼을 빈 문자열과 비교하면
     * DB·Hibernate 조합에 따라 실제 상세 데이터가 있어도
     * 0건으로 집계되는 문제가 생길 수 있다.
     *
     * 상태 API 전용 집계는 MySQL native query로 처리한다.
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