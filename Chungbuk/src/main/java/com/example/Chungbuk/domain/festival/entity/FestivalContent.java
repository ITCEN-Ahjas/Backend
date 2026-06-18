package com.example.Chungbuk.domain.festival.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "festival_contents",
        indexes = {
                @Index(name = "idx_festival_content_id", columnList = "content_id", unique = true),
                @Index(name = "idx_festival_content_type_id", columnList = "content_type_id"),
                @Index(name = "idx_festival_region", columnList = "region"),
                @Index(name = "idx_festival_category", columnList = "category"),
                @Index(name = "idx_festival_theme_category", columnList = "theme_category"),
                @Index(name = "idx_festival_start_date", columnList = "start_date"),
                @Index(name = "idx_festival_end_date", columnList = "end_date"),
                @Index(name = "idx_festival_active", columnList = "is_active")
        }
)
public class FestivalContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * TourAPI 콘텐츠 ID.
     * DB 내부 PK와 별도로 외부 공공데이터 식별자로 사용한다.
     */
    @Column(name = "content_id", nullable = false, unique = true, length = 50)
    private String contentId;

    /*
     * TourAPI 콘텐츠 타입.
     * 12: 관광지
     * 14: 문화시설
     * 15: 행사/공연/축제
     * 28: 레포츠
     */
    @Column(name = "content_type_id", length = 20)
    private String contentTypeId;

    @Column(name = "cat1", length = 30)
    private String cat1;

    @Column(name = "cat2", length = 30)
    private String cat2;

    @Column(name = "cat3", length = 30)
    private String cat3;

    @Column(name = "title", length = 300)
    private String title;

    @Column(name = "region", length = 50)
    private String region;

    /*
     * 메인 카테고리.
     * 예: 관광지, 문화시설, 행사, 공연, 축제, 레포츠
     */
    @Column(name = "category", length = 50)
    private String category;

    /*
     * 보조 테마 카테고리.
     * 예: 먹거리, 야간행사, 자연관광, 역사문화, 캠핑 등
     */
    @Column(name = "theme_category", length = 80)
    private String themeCategory;

    @Column(name = "status", length = 30)
    private String status;

    /*
     * TourAPI 날짜 형식 유지.
     * 예: 20250912
     */
    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "end_date", length = 20)
    private String endDate;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    /*
     * 상세 이미지 목록은 JSON 문자열로 저장한다.
     * 예: ["url1", "url2"]
     */
    @Lob
    @Column(name = "image_urls_json", columnDefinition = "TEXT")
    private String imageUrlsJson;

    @Column(name = "tel", length = 100)
    private String tel;

    @Column(name = "homepage", length = 1000)
    private String homepage;

    @Lob
    @Column(name = "overview", columnDefinition = "TEXT")
    private String overview;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "description_source", length = 80)
    private String descriptionSource;

    @Column(name = "map_x", length = 50)
    private String mapX;

    @Column(name = "map_y", length = 50)
    private String mapY;

    @Column(name = "event_place", length = 500)
    private String eventPlace;

    @Column(name = "play_time", length = 500)
    private String playTime;

    @Column(name = "use_time_festival", length = 500)
    private String useTimeFestival;

    @Column(name = "sponsor", length = 300)
    private String sponsor;

    @Column(name = "time_label", length = 80)
    private String timeLabel;

    @Column(name = "time_value", length = 500)
    private String timeValue;

    @Column(name = "extra_label", length = 80)
    private String extraLabel;

    @Column(name = "extra_value", length = 500)
    private String extraValue;

    /*
     * 상세페이지 주요 정보 목록은 JSON 문자열로 저장한다.
     * 예: [{"label":"축제 기간","value":"2025.09.12 ~ 2025.09.14"}]
     */
    @Lob
    @Column(name = "main_info_json", columnDefinition = "TEXT")
    private String mainInfoJson;

    /*
     * 공공데이터에서 일시적으로 누락되거나 삭제된 경우 바로 delete하지 않고 비활성화한다.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean active;

    /*
     * 우리 서버가 TourAPI에서 마지막으로 동기화한 시간.
     */
    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    /*
     * TourAPI 원본 수정 시간이 제공되는 경우 저장한다.
     * 원본 수정 시간이 없으면 null로 둔다.
     */
    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (active == null) {
            active = true;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateLastSyncedAt(LocalDateTime lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public void updateSourceUpdatedAt(LocalDateTime sourceUpdatedAt) {
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public void updateSummaryInfo(
            String contentTypeId,
            String cat1,
            String cat2,
            String cat3,
            String title,
            String region,
            String category,
            String themeCategory,
            String status,
            String startDate,
            String endDate,
            String address,
            String imageUrl,
            String tel,
            String mapX,
            String mapY,
            String timeLabel,
            String timeValue,
            String extraLabel,
            String extraValue,
            LocalDateTime lastSyncedAt
    ) {
        this.contentTypeId = contentTypeId;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.title = title;
        this.region = region;
        this.category = category;
        this.themeCategory = themeCategory;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.address = address;
        this.imageUrl = imageUrl;
        this.tel = tel;
        this.mapX = mapX;
        this.mapY = mapY;
        this.timeLabel = timeLabel;
        this.timeValue = timeValue;
        this.extraLabel = extraLabel;
        this.extraValue = extraValue;
        this.lastSyncedAt = lastSyncedAt;
        this.active = true;
    }

    public void updateDetailInfo(
            String contentTypeId,
            String cat1,
            String cat2,
            String cat3,
            String title,
            String region,
            String category,
            String themeCategory,
            String status,
            String startDate,
            String endDate,
            String address,
            String imageUrl,
            String imageUrlsJson,
            String tel,
            String homepage,
            String overview,
            String description,
            String descriptionSource,
            String mapX,
            String mapY,
            String eventPlace,
            String playTime,
            String useTimeFestival,
            String sponsor,
            String timeLabel,
            String timeValue,
            String extraLabel,
            String extraValue,
            String mainInfoJson,
            LocalDateTime lastSyncedAt,
            LocalDateTime sourceUpdatedAt
    ) {
        this.contentTypeId = contentTypeId;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.title = title;
        this.region = region;
        this.category = category;
        this.themeCategory = themeCategory;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.address = address;
        this.imageUrl = imageUrl;
        this.imageUrlsJson = imageUrlsJson;
        this.tel = tel;
        this.homepage = homepage;
        this.overview = overview;
        this.description = description;
        this.descriptionSource = descriptionSource;
        this.mapX = mapX;
        this.mapY = mapY;
        this.eventPlace = eventPlace;
        this.playTime = playTime;
        this.useTimeFestival = useTimeFestival;
        this.sponsor = sponsor;
        this.timeLabel = timeLabel;
        this.timeValue = timeValue;
        this.extraLabel = extraLabel;
        this.extraValue = extraValue;
        this.mainInfoJson = mainInfoJson;
        this.lastSyncedAt = lastSyncedAt;
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.active = true;
    }

    public void updateContent(
            String contentTypeId,
            String cat1,
            String cat2,
            String cat3,
            String title,
            String region,
            String category,
            String themeCategory,
            String status,
            String startDate,
            String endDate,
            String address,
            String imageUrl,
            String imageUrlsJson,
            String tel,
            String homepage,
            String overview,
            String description,
            String descriptionSource,
            String mapX,
            String mapY,
            String eventPlace,
            String playTime,
            String useTimeFestival,
            String sponsor,
            String timeLabel,
            String timeValue,
            String extraLabel,
            String extraValue,
            String mainInfoJson,
            LocalDateTime lastSyncedAt,
            LocalDateTime sourceUpdatedAt
    ) {
        updateDetailInfo(
                contentTypeId,
                cat1,
                cat2,
                cat3,
                title,
                region,
                category,
                themeCategory,
                status,
                startDate,
                endDate,
                address,
                imageUrl,
                imageUrlsJson,
                tel,
                homepage,
                overview,
                description,
                descriptionSource,
                mapX,
                mapY,
                eventPlace,
                playTime,
                useTimeFestival,
                sponsor,
                timeLabel,
                timeValue,
                extraLabel,
                extraValue,
                mainInfoJson,
                lastSyncedAt,
                sourceUpdatedAt
        );
    }
}