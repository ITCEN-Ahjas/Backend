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