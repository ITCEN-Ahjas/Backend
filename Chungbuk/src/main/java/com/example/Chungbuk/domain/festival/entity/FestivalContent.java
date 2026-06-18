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

    @Column(name = "content_id", nullable = false, unique = true, length = 50)
    private String contentId;

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

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "theme_category", length = 80)
    private String themeCategory;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "start_date", length = 20)
    private String startDate;

    @Column(name = "end_date", length = 20)
    private String endDate;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

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

    @Lob
    @Column(name = "main_info_json", columnDefinition = "TEXT")
    private String mainInfoJson;

    @Column(name = "is_active", nullable = false)
    private Boolean active;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    /* TourAPI 목록 응답의 modifiedtime */
    @Column(name = "source_updated_at")
    private LocalDateTime sourceUpdatedAt;

    /* 마지막으로 detailCommon2/detailIntro2까지 성공 저장한 원본 수정 시각 */
    @Column(name = "detail_source_updated_at")
    private LocalDateTime detailSourceUpdatedAt;

    /* detailImage2까지 정상 응답을 받아 이미지 목록을 확정했는지 여부 */
    @Column(name = "image_sync_completed")
    private Boolean imageSyncCompleted;

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

        if (imageSyncCompleted == null) {
            imageSyncCompleted = false;
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
        if (sourceUpdatedAt != null) {
            this.sourceUpdatedAt = sourceUpdatedAt;
        }
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
        updateSummaryInfo(
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
                tel,
                mapX,
                mapY,
                timeLabel,
                timeValue,
                extraLabel,
                extraValue,
                lastSyncedAt,
                null
        );
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
            LocalDateTime lastSyncedAt,
            LocalDateTime sourceUpdatedAt
    ) {
        this.contentTypeId = chooseContentTypeId(contentTypeId, this.contentTypeId);
        this.cat1 = chooseText(cat1, this.cat1);
        this.cat2 = chooseText(cat2, this.cat2);
        this.cat3 = chooseText(cat3, this.cat3);
        this.title = chooseText(title, this.title);
        this.region = chooseText(region, this.region);
        this.category = chooseText(category, this.category);
        this.themeCategory = chooseText(themeCategory, this.themeCategory);
        this.status = chooseText(status, this.status);
        this.startDate = chooseText(startDate, this.startDate);
        this.endDate = chooseText(endDate, this.endDate);
        this.address = chooseText(address, this.address);
        this.imageUrl = chooseText(imageUrl, this.imageUrl);
        this.tel = chooseText(tel, this.tel);
        this.mapX = chooseText(mapX, this.mapX);
        this.mapY = chooseText(mapY, this.mapY);

        updateDisplayInfoPreservingUsefulValue(
                timeLabel,
                timeValue,
                extraLabel,
                extraValue
        );

        this.lastSyncedAt = lastSyncedAt;
        updateSourceUpdatedAt(sourceUpdatedAt);
        this.active = true;
    }

    public void initializeDailyRefreshBaseline(
            LocalDateTime sourceUpdatedAt
    ) {
        if (hasStoredDetail() && detailSourceUpdatedAt == null
                && sourceUpdatedAt != null) {
            detailSourceUpdatedAt = sourceUpdatedAt;
        }

        if (hasStoredDetail() && imageSyncCompleted == null) {
            imageSyncCompleted = true;
        }
    }

    public void updateCardInfo(
            String timeLabel,
            String timeValue,
            String extraLabel,
            String extraValue,
            LocalDateTime lastSyncedAt
    ) {
        replaceDisplayInfo(
                timeLabel,
                timeValue,
                extraLabel,
                extraValue
        );

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
                sourceUpdatedAt,
                sourceUpdatedAt,
                true
        );
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
            LocalDateTime sourceUpdatedAt,
            LocalDateTime detailSourceUpdatedAt,
            Boolean imageSyncCompleted
    ) {
        boolean incomingTypeMismatch = hasText(this.contentTypeId)
                && hasText(contentTypeId)
                && !this.contentTypeId.equals(contentTypeId);

        this.contentTypeId = chooseContentTypeId(contentTypeId, this.contentTypeId);

        if (incomingTypeMismatch) {
            this.cat1 = chooseText(this.cat1, cat1);
            this.cat2 = chooseText(this.cat2, cat2);
            this.cat3 = chooseText(this.cat3, cat3);
            this.category = chooseText(this.category, category);
            this.themeCategory = chooseText(this.themeCategory, themeCategory);
            this.status = chooseText(this.status, status);
            this.startDate = chooseText(this.startDate, startDate);
            this.endDate = chooseText(this.endDate, endDate);
        } else {
            this.cat1 = chooseText(cat1, this.cat1);
            this.cat2 = chooseText(cat2, this.cat2);
            this.cat3 = chooseText(cat3, this.cat3);
            this.category = chooseText(category, this.category);
            this.themeCategory = chooseText(themeCategory, this.themeCategory);
            this.status = chooseText(status, this.status);
            this.startDate = chooseText(startDate, this.startDate);
            this.endDate = chooseText(endDate, this.endDate);
        }

        this.title = chooseText(title, this.title);
        this.region = chooseText(region, this.region);
        this.address = chooseText(address, this.address);
        this.imageUrl = chooseText(imageUrl, this.imageUrl);
        this.tel = chooseText(tel, this.tel);
        this.homepage = chooseText(homepage, this.homepage);
        this.overview = chooseText(overview, this.overview);
        this.description = chooseText(description, this.description);
        this.descriptionSource = chooseText(descriptionSource, this.descriptionSource);
        this.mapX = chooseText(mapX, this.mapX);
        this.mapY = chooseText(mapY, this.mapY);
        this.eventPlace = chooseText(eventPlace, this.eventPlace);
        this.playTime = chooseText(playTime, this.playTime);
        this.useTimeFestival = chooseText(useTimeFestival, this.useTimeFestival);
        this.sponsor = chooseText(sponsor, this.sponsor);

        updateDisplayInfoPreservingUsefulValue(
                timeLabel,
                timeValue,
                extraLabel,
                extraValue
        );

        this.mainInfoJson = chooseJsonArray(mainInfoJson, this.mainInfoJson);
        this.lastSyncedAt = lastSyncedAt;
        updateSourceUpdatedAt(sourceUpdatedAt);

        if (detailSourceUpdatedAt != null) {
            this.detailSourceUpdatedAt = detailSourceUpdatedAt;
        }

        if (imageSyncCompleted != null) {
            this.imageSyncCompleted = imageSyncCompleted;

            if (imageSyncCompleted) {
                this.imageUrlsJson = normalizeJsonArray(imageUrlsJson);
            }
        }

        this.active = true;
    }

    public void updateImageInfo(
            String imageUrlsJson,
            LocalDateTime lastSyncedAt
    ) {
        this.imageUrlsJson = normalizeJsonArray(imageUrlsJson);
        this.imageSyncCompleted = true;
        this.lastSyncedAt = lastSyncedAt;
        this.active = true;
    }

    public void markDetailCommonUnavailable(
            LocalDateTime lastSyncedAt,
            LocalDateTime sourceUpdatedAt
    ) {
        this.descriptionSource = "DETAIL_COMMON_EMPTY";
        this.lastSyncedAt = lastSyncedAt;
        updateSourceUpdatedAt(sourceUpdatedAt);

        if (sourceUpdatedAt != null) {
            this.detailSourceUpdatedAt = sourceUpdatedAt;
        }

        this.imageSyncCompleted = true;
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

    private boolean hasStoredDetail() {
        return hasText(overview)
                || hasText(homepage)
                || hasText(description)
                || hasText(descriptionSource)
                || hasMeaningfulJsonArray(mainInfoJson)
                || detailSourceUpdatedAt != null;
    }

    private void updateDisplayInfoPreservingUsefulValue(
            String newTimeLabel,
            String newTimeValue,
            String newExtraLabel,
            String newExtraValue
    ) {
        if (isUsefulDisplayInfo(newTimeLabel, newTimeValue)) {
            this.timeLabel = newTimeLabel;
            this.timeValue = newTimeValue;
        } else if (!isUsefulDisplayInfo(this.timeLabel, this.timeValue)) {
            this.timeLabel = "";
            this.timeValue = "";
        }

        if (isUsefulDisplayInfo(newExtraLabel, newExtraValue)) {
            this.extraLabel = newExtraLabel;
            this.extraValue = newExtraValue;
        } else if (!isUsefulDisplayInfo(this.extraLabel, this.extraValue)) {
            this.extraLabel = "";
            this.extraValue = "";
        }
    }

    private void replaceDisplayInfo(
            String newTimeLabel,
            String newTimeValue,
            String newExtraLabel,
            String newExtraValue
    ) {
        if (isUsefulDisplayInfo(newTimeLabel, newTimeValue)) {
            this.timeLabel = newTimeLabel;
            this.timeValue = newTimeValue;
        } else {
            this.timeLabel = "";
            this.timeValue = "";
        }

        if (isUsefulDisplayInfo(newExtraLabel, newExtraValue)) {
            this.extraLabel = newExtraLabel;
            this.extraValue = newExtraValue;
        } else {
            this.extraLabel = "";
            this.extraValue = "";
        }
    }

    private String chooseContentTypeId(String newValue, String currentValue) {
        if (hasText(currentValue)
                && hasText(newValue)
                && "15".equals(newValue)
                && !"15".equals(currentValue)) {
            return currentValue;
        }

        return chooseText(newValue, currentValue);
    }

    private String chooseText(String newValue, String currentValue) {
        if (hasText(newValue)) {
            return newValue;
        }

        return currentValue;
    }

    private String chooseJsonArray(String newValue, String currentValue) {
        if (hasMeaningfulJsonArray(newValue)) {
            return newValue;
        }

        return currentValue;
    }

    private String normalizeJsonArray(String value) {
        return hasText(value) ? value : "[]";
    }

    private boolean hasMeaningfulJsonArray(String value) {
        return hasText(value) && !"[]".equals(value.trim());
    }

    private boolean isUsefulDisplayInfo(String label, String value) {
        return hasText(label) && isUsefulText(value);
    }

    private boolean isUsefulText(String value) {
        if (!hasText(value)) {
            return false;
        }

        String normalized = value.trim();

        return !normalized.equals("정보 없음")
                && !normalized.equals("상세페이지에서 확인")
                && !normalized.equals("제공된 상세 설명이 없습니다.")
                && !normalized.equals("분류");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
