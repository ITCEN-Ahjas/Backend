package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import com.example.Chungbuk.domain.festival.mapper.FestivalMapper;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FestivalSyncService {

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;
    private final FestivalContentRepository festivalContentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 30;
    private static final int MAX_DETAIL_SYNC_COUNT = 3;
    private static final int MAX_TOUR_API_CALL_COUNT = 100;
    private static final int DETAIL_FALLBACK_SIZE = 100;
    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final String DEFAULT_EXPERIENCE_CONTENT_TYPE_ID = "";
    private static final String DETAIL_FALLBACK_EVENT_START_DATE = "20230101";
    private static final DateTimeFormatter TOUR_API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional
    public FestivalSyncResultResponse syncFestivalContents(
            int festivalPage,
            int festivalSize,
            int experiencePage,
            int experienceSize,
            String eventStartDate,
            boolean includeDetail
    ) {
        int validFestivalPage = normalizePage(festivalPage);
        int validFestivalSize = normalizeSize(festivalSize);
        int validExperiencePage = normalizePage(experiencePage);
        int validExperienceSize = normalizeSize(experienceSize);
        String validEventStartDate = normalizeEventStartDate(eventStartDate);

        LocalDateTime syncedAt = LocalDateTime.now();
        SyncCounter counter = new SyncCounter();

        String festivalRawJson = tourApiClient.getFestivalListRaw(
                validFestivalPage,
                validFestivalSize,
                validEventStartDate,
                null
        );
        counter.increaseTourApiCallCount();

        FestivalListResponse festivalListResponse = festivalMapper.toFestivalListResponse(
                festivalRawJson,
                validFestivalPage,
                validFestivalSize
        );

        for (FestivalSummaryResponse item : festivalListResponse.getItems()) {
            upsertFestivalSummary(item, syncedAt, counter);
        }

        String experienceRawJson = tourApiClient.getExperienceListRaw(
                validExperiencePage,
                validExperienceSize,
                null,
                DEFAULT_EXPERIENCE_CONTENT_TYPE_ID
        );
        counter.increaseTourApiCallCount();

        ExperienceListResponse experienceListResponse = festivalMapper.toExperienceListResponse(
                experienceRawJson,
                validExperiencePage,
                validExperienceSize
        );

        for (ExperienceSummaryResponse item : experienceListResponse.getItems()) {
            upsertExperienceSummary(item, syncedAt, counter);
        }

        if (includeDetail) {
            syncDetailsWithinLimit(
                    festivalListResponse.getItems(),
                    experienceListResponse.getItems(),
                    syncedAt,
                    counter
            );
        } else {
            counter.addSkippedDetailCount(
                    festivalListResponse.getItems().size() + experienceListResponse.getItems().size()
            );
        }

        return FestivalSyncResultResponse.builder()
                .festivalSavedCount(counter.getFestivalSavedCount())
                .experienceSavedCount(counter.getExperienceSavedCount())
                .detailSyncedCount(counter.getDetailSyncedCount())
                .insertedCount(counter.getInsertedCount())
                .updatedCount(counter.getUpdatedCount())
                .tourApiCallCount(counter.getTourApiCallCount())
                .skippedDetailCount(counter.getSkippedDetailCount())
                .festivalPage(validFestivalPage)
                .festivalSize(validFestivalSize)
                .experiencePage(validExperiencePage)
                .experienceSize(validExperienceSize)
                .includeDetail(includeDetail)
                .eventStartDate(validEventStartDate)
                .message(buildMessage(includeDetail, counter))
                .build();
    }

    @Transactional
    public FestivalSyncResultResponse syncFestivalContentDetail(String contentId) {
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncedAt = LocalDateTime.now();

        syncDetail(contentId, syncedAt, counter);

        return FestivalSyncResultResponse.builder()
                .festivalSavedCount(0)
                .experienceSavedCount(0)
                .detailSyncedCount(counter.getDetailSyncedCount())
                .insertedCount(counter.getInsertedCount())
                .updatedCount(counter.getUpdatedCount())
                .tourApiCallCount(counter.getTourApiCallCount())
                .skippedDetailCount(counter.getSkippedDetailCount())
                .festivalPage(0)
                .festivalSize(0)
                .experiencePage(0)
                .experienceSize(0)
                .includeDetail(true)
                .eventStartDate("")
                .message("contentId 기준으로 TourAPI 상세 데이터를 DB에 저장/갱신했습니다.")
                .build();
    }

    private void syncDetailsWithinLimit(
            List<FestivalSummaryResponse> festivalItems,
            List<ExperienceSummaryResponse> experienceItems,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        int detailSyncCount = 0;

        for (FestivalSummaryResponse item : festivalItems) {
            if (detailSyncCount >= MAX_DETAIL_SYNC_COUNT || !canCallDetailApis(counter)) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            syncDetail(item.getId(), syncedAt, counter);
            detailSyncCount++;
        }

        for (ExperienceSummaryResponse item : experienceItems) {
            if (detailSyncCount >= MAX_DETAIL_SYNC_COUNT || !canCallDetailApis(counter)) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            syncDetail(item.getId(), syncedAt, counter);
            detailSyncCount++;
        }
    }

    private boolean canCallDetailApis(SyncCounter counter) {
        return counter.getTourApiCallCount() + 4 <= MAX_TOUR_API_CALL_COUNT;
    }

    private void syncDetail(
            String contentId,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        if (!hasText(contentId)) {
            counter.increaseSkippedDetailCount();
            return;
        }

        String detailCommonRawJson = tourApiClient.getFestivalDetailCommonRaw(contentId);
        counter.increaseTourApiCallCount();

        String fallbackListRawJson = tourApiClient.getFestivalListRaw(
                DEFAULT_PAGE,
                DETAIL_FALLBACK_SIZE,
                DETAIL_FALLBACK_EVENT_START_DATE,
                null
        );
        counter.increaseTourApiCallCount();

        String contentTypeId = firstNonBlank(
                festivalMapper.extractContentTypeId(detailCommonRawJson),
                festivalMapper.extractContentTypeIdFromFallback(fallbackListRawJson, contentId),
                DEFAULT_FESTIVAL_CONTENT_TYPE_ID
        );

        String detailIntroRawJson = tourApiClient.getFestivalDetailIntroRaw(
                contentId,
                contentTypeId
        );
        counter.increaseTourApiCallCount();

        String detailImageRawJson = tourApiClient.getFestivalDetailImageRaw(contentId);
        counter.increaseTourApiCallCount();

        FestivalDetailResponse detailResponse = festivalMapper.toFestivalDetailResponse(
                detailCommonRawJson,
                detailIntroRawJson,
                detailImageRawJson,
                fallbackListRawJson,
                contentId
        );

        upsertDetail(detailResponse, syncedAt, counter);
    }

    private void upsertFestivalSummary(
            FestivalSummaryResponse item,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        if (item == null || !hasText(item.getId())) {
            return;
        }

        FestivalContent content = festivalContentRepository.findByContentId(item.getId())
                .orElseGet(() -> {
                    counter.increaseInsertedCount();

                    return FestivalContent.builder()
                            .contentId(item.getId())
                            .active(true)
                            .build();
                });

        if (content.getId() != null) {
            counter.increaseUpdatedCount();
        }

        content.updateSummaryInfo(
                item.getContentTypeId(),
                item.getCat1(),
                item.getCat2(),
                item.getCat3(),
                item.getTitle(),
                item.getRegion(),
                item.getCategory(),
                item.getThemeCategory(),
                item.getStatus(),
                item.getStartDate(),
                item.getEndDate(),
                item.getAddress(),
                item.getImageUrl(),
                item.getTel(),
                item.getMapX(),
                item.getMapY(),
                item.getTimeLabel(),
                item.getTimeValue(),
                item.getExtraLabel(),
                item.getExtraValue(),
                syncedAt
        );

        festivalContentRepository.save(content);
        counter.increaseFestivalSavedCount();
    }

    private void upsertExperienceSummary(
            ExperienceSummaryResponse item,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        if (item == null || !hasText(item.getId())) {
            return;
        }

        FestivalContent content = festivalContentRepository.findByContentId(item.getId())
                .orElseGet(() -> {
                    counter.increaseInsertedCount();

                    return FestivalContent.builder()
                            .contentId(item.getId())
                            .active(true)
                            .build();
                });

        if (content.getId() != null) {
            counter.increaseUpdatedCount();
        }

        content.updateSummaryInfo(
                item.getContentTypeId(),
                item.getCat1(),
                item.getCat2(),
                item.getCat3(),
                item.getTitle(),
                item.getRegion(),
                item.getCategory(),
                item.getThemeCategory(),
                "",
                "",
                "",
                item.getAddress(),
                item.getImageUrl(),
                item.getTel(),
                item.getMapX(),
                item.getMapY(),
                item.getTimeLabel(),
                item.getTimeValue(),
                item.getExtraLabel(),
                item.getExtraValue(),
                syncedAt
        );

        festivalContentRepository.save(content);
        counter.increaseExperienceSavedCount();
    }

    private void upsertDetail(
            FestivalDetailResponse detail,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        if (detail == null || !hasText(detail.getId())) {
            counter.increaseSkippedDetailCount();
            return;
        }

        FestivalContent content = festivalContentRepository.findByContentId(detail.getId())
                .orElseGet(() -> {
                    counter.increaseInsertedCount();

                    return FestivalContent.builder()
                            .contentId(detail.getId())
                            .active(true)
                            .build();
                });

        if (content.getId() != null) {
            counter.increaseUpdatedCount();
        }

        content.updateDetailInfo(
                detail.getContentTypeId(),
                detail.getCat1(),
                detail.getCat2(),
                detail.getCat3(),
                detail.getTitle(),
                detail.getRegion(),
                detail.getCategory(),
                detail.getThemeCategory(),
                detail.getStatus(),
                detail.getStartDate(),
                detail.getEndDate(),
                detail.getAddress(),
                detail.getImageUrl(),
                toJson(detail.getImageUrls()),
                detail.getTel(),
                detail.getHomepage(),
                detail.getOverview(),
                detail.getDescription(),
                detail.getDescriptionSource(),
                detail.getMapX(),
                detail.getMapY(),
                detail.getEventPlace(),
                detail.getPlayTime(),
                detail.getUseTimeFestival(),
                detail.getSponsor(),
                detail.getTimeLabel(),
                detail.getTimeValue(),
                detail.getExtraLabel(),
                detail.getExtraValue(),
                toJson(detail.getMainInfo()),
                syncedAt,
                null
        );

        festivalContentRepository.save(content);
        counter.increaseDetailSyncedCount();
    }

    private String toJson(Object value) {
        if (value == null) {
            return "[]";
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String buildMessage(boolean includeDetail, SyncCounter counter) {
        if (includeDetail) {
            if (counter.getDetailSyncedCount() > 0) {
                return "TourAPI 데이터를 DB에 저장/갱신했습니다. 상세 데이터는 트래픽 제한을 고려하여 일부만 보강했습니다.";
            }

            return "TourAPI 목록 데이터를 DB에 저장/갱신했습니다. 상세 보강 대상이 없어서 상세 데이터는 저장되지 않았습니다.";
        }

        return "TourAPI 목록 데이터를 DB에 저장/갱신했습니다. 상세 데이터 보강은 contentId 단건 상세 동기화 API에서 수행할 수 있습니다.";
    }

    private int normalizePage(int page) {
        if (page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }

        if (size > MAX_SIZE) {
            return MAX_SIZE;
        }

        return size;
    }

    private String normalizeEventStartDate(String eventStartDate) {
        if (hasText(eventStartDate) && eventStartDate.trim().matches("\\d{8}")) {
            return eventStartDate.trim();
        }

        return LocalDate.now().format(TOUR_API_DATE_FORMAT);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }

        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static class SyncCounter {

        private int festivalSavedCount;
        private int experienceSavedCount;
        private int detailSyncedCount;

        private int insertedCount;
        private int updatedCount;

        private int tourApiCallCount;
        private int skippedDetailCount;

        public int getFestivalSavedCount() {
            return festivalSavedCount;
        }

        public int getExperienceSavedCount() {
            return experienceSavedCount;
        }

        public int getDetailSyncedCount() {
            return detailSyncedCount;
        }

        public int getInsertedCount() {
            return insertedCount;
        }

        public int getUpdatedCount() {
            return updatedCount;
        }

        public int getTourApiCallCount() {
            return tourApiCallCount;
        }

        public int getSkippedDetailCount() {
            return skippedDetailCount;
        }

        public void increaseFestivalSavedCount() {
            festivalSavedCount++;
        }

        public void increaseExperienceSavedCount() {
            experienceSavedCount++;
        }

        public void increaseDetailSyncedCount() {
            detailSyncedCount++;
        }

        public void increaseInsertedCount() {
            insertedCount++;
        }

        public void increaseUpdatedCount() {
            updatedCount++;
        }

        public void increaseTourApiCallCount() {
            tourApiCallCount++;
        }

        public void increaseSkippedDetailCount() {
            skippedDetailCount++;
        }

        public void addSkippedDetailCount(int count) {
            skippedDetailCount += count;
        }
    }
}