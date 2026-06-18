package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncStatusResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalContent;
import com.example.Chungbuk.domain.festival.mapper.FestivalMapper;
import com.example.Chungbuk.domain.festival.repository.FestivalContentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private static final int DEFAULT_DETAIL_SYNC_LIMIT = 30;
    private static final int MAX_DETAIL_SYNC_LIMIT = 50;

    private static final int DEFAULT_MAX_AUTO_SYNC_PAGES = 20;
    private static final int MAX_AUTO_SYNC_PAGES = 50;

    private static final int MAX_TOUR_API_CALL_COUNT = 100;
    private static final int DETAIL_FALLBACK_SIZE = 100;

    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final String DEFAULT_EXPERIENCE_CONTENT_TYPE_ID = "";
    private static final String DETAIL_FALLBACK_EVENT_START_DATE = "20230101";

    private static final List<String> AUTO_SYNC_EXPERIENCE_CONTENT_TYPE_IDS = List.of("12", "14", "28");

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Transactional(readOnly = true)
    public FestivalSyncStatusResponse getFestivalSyncStatus() {
        long totalCount = festivalContentRepository.countByActiveTrue();

        long festivalCount = festivalContentRepository.countByContentTypeIdAndActiveTrue("15");
        long touristSpotCount = festivalContentRepository.countByContentTypeIdAndActiveTrue("12");
        long cultureCount = festivalContentRepository.countByContentTypeIdAndActiveTrue("14");
        long leportsCount = festivalContentRepository.countByContentTypeIdAndActiveTrue("28");

        long detailSyncedCount = festivalContentRepository.countDetailSyncedContents();
        LocalDateTime lastSyncedAt = festivalContentRepository.findLatestSyncedAt();

        Map<String, Long> contentTypeCounts = toCountMap(
                festivalContentRepository.countActiveContentsByContentTypeId()
        );

        Map<String, Long> categoryCounts = toCountMap(
                festivalContentRepository.countActiveContentsByCategory()
        );

        return FestivalSyncStatusResponse.builder()
                .totalCount(totalCount)
                .festivalCount(festivalCount)
                .touristSpotCount(touristSpotCount)
                .cultureCount(cultureCount)
                .leportsCount(leportsCount)
                .detailSyncedCount(detailSyncedCount)
                .lastSyncedAt(lastSyncedAt)
                .empty(totalCount == 0)
                .contentTypeCounts(contentTypeCounts)
                .categoryCounts(categoryCounts)
                .build();
    }

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

        SyncPageResult<FestivalSummaryResponse> festivalPageResult = syncFestivalPage(
                validFestivalPage,
                validFestivalSize,
                validEventStartDate,
                syncedAt,
                counter
        );

        SyncPageResult<ExperienceSummaryResponse> experiencePageResult = syncExperiencePage(
                validExperiencePage,
                validExperienceSize,
                DEFAULT_EXPERIENCE_CONTENT_TYPE_ID,
                syncedAt,
                counter
        );

        syncDetailIfRequested(
                festivalPageResult.items(),
                experiencePageResult.items(),
                includeDetail,
                syncedAt,
                counter
        );

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
                .festivalStartPage(validFestivalPage)
                .festivalEndPage(validFestivalPage)
                .experienceStartPage(validExperiencePage)
                .experienceEndPage(validExperiencePage)
                .maxPages(0)
                .detailLimit(includeDetail ? MAX_DETAIL_SYNC_COUNT : 0)
                .includeDetail(includeDetail)
                .automaticSync(false)
                .eventStartDate(validEventStartDate)
                .message(buildMessage(includeDetail, counter))
                .build();
    }

    @Transactional
    public FestivalSyncResultResponse syncFestivalContentsBulk(
            int festivalStartPage,
            int festivalEndPage,
            int festivalSize,
            int experienceStartPage,
            int experienceEndPage,
            int experienceSize,
            String eventStartDate,
            boolean includeDetail
    ) {
        int validFestivalStartPage = normalizePage(festivalStartPage);
        int validFestivalEndPage = normalizeEndPage(festivalEndPage, validFestivalStartPage);
        int validFestivalSize = normalizeSize(festivalSize);

        int validExperienceStartPage = normalizePage(experienceStartPage);
        int validExperienceEndPage = normalizeEndPage(experienceEndPage, validExperienceStartPage);
        int validExperienceSize = normalizeSize(experienceSize);

        String validEventStartDate = normalizeEventStartDate(eventStartDate);

        LocalDateTime syncedAt = LocalDateTime.now();
        SyncCounter counter = new SyncCounter();

        List<FestivalSummaryResponse> festivalItems = new ArrayList<>();
        List<ExperienceSummaryResponse> experienceItems = new ArrayList<>();

        int lastFestivalPage = validFestivalStartPage;
        int lastExperiencePage = validExperienceStartPage;

        for (int page = validFestivalStartPage; page <= validFestivalEndPage; page++) {
            if (!canCallListApi(counter)) {
                break;
            }

            SyncPageResult<FestivalSummaryResponse> pageResult = syncFestivalPage(
                    page,
                    validFestivalSize,
                    validEventStartDate,
                    syncedAt,
                    counter
            );

            festivalItems.addAll(pageResult.items());
            lastFestivalPage = page;
        }

        for (int page = validExperienceStartPage; page <= validExperienceEndPage; page++) {
            if (!canCallListApi(counter)) {
                break;
            }

            SyncPageResult<ExperienceSummaryResponse> pageResult = syncExperiencePage(
                    page,
                    validExperienceSize,
                    DEFAULT_EXPERIENCE_CONTENT_TYPE_ID,
                    syncedAt,
                    counter
            );

            experienceItems.addAll(pageResult.items());
            lastExperiencePage = page;
        }

        syncDetailIfRequested(
                festivalItems,
                experienceItems,
                includeDetail,
                syncedAt,
                counter
        );

        return FestivalSyncResultResponse.builder()
                .festivalSavedCount(counter.getFestivalSavedCount())
                .experienceSavedCount(counter.getExperienceSavedCount())
                .detailSyncedCount(counter.getDetailSyncedCount())
                .insertedCount(counter.getInsertedCount())
                .updatedCount(counter.getUpdatedCount())
                .tourApiCallCount(counter.getTourApiCallCount())
                .skippedDetailCount(counter.getSkippedDetailCount())
                .festivalPage(validFestivalStartPage)
                .festivalSize(validFestivalSize)
                .experiencePage(validExperienceStartPage)
                .experienceSize(validExperienceSize)
                .festivalStartPage(validFestivalStartPage)
                .festivalEndPage(lastFestivalPage)
                .experienceStartPage(validExperienceStartPage)
                .experienceEndPage(lastExperiencePage)
                .maxPages(0)
                .detailLimit(includeDetail ? MAX_DETAIL_SYNC_COUNT : 0)
                .includeDetail(includeDetail)
                .automaticSync(false)
                .eventStartDate(validEventStartDate)
                .message(buildBulkMessage(includeDetail, counter))
                .build();
    }

    @Transactional
    public FestivalSyncResultResponse syncFestivalContentsAll(
            int size,
            int maxPages,
            String eventStartDate,
            boolean includeDetail,
            int detailLimit
    ) {
        int validSize = normalizeSize(size);
        int validMaxPages = normalizeMaxPages(maxPages);
        int validDetailLimit = normalizeDetailLimit(detailLimit);
        String validEventStartDate = normalizeEventStartDate(eventStartDate);

        LocalDateTime syncedAt = LocalDateTime.now();
        SyncCounter counter = new SyncCounter();

        List<FestivalSummaryResponse> festivalItems = new ArrayList<>();
        List<ExperienceSummaryResponse> experienceItems = new ArrayList<>();

        int lastFestivalPage = syncFestivalAllPages(
                validSize,
                validMaxPages,
                validEventStartDate,
                syncedAt,
                counter,
                festivalItems
        );

        int lastExperiencePage = 0;

        for (String contentTypeId : AUTO_SYNC_EXPERIENCE_CONTENT_TYPE_IDS) {
            int lastPage = syncExperienceAllPages(
                    contentTypeId,
                    validSize,
                    validMaxPages,
                    syncedAt,
                    counter,
                    experienceItems
            );

            lastExperiencePage = Math.max(lastExperiencePage, lastPage);
        }

        if (includeDetail) {
            syncDetailsWithinLimit(
                    festivalItems,
                    experienceItems,
                    syncedAt,
                    counter,
                    validDetailLimit,
                    true
            );
        } else {
            counter.addSkippedDetailCount(festivalItems.size() + experienceItems.size());
        }

        return FestivalSyncResultResponse.builder()
                .festivalSavedCount(counter.getFestivalSavedCount())
                .experienceSavedCount(counter.getExperienceSavedCount())
                .detailSyncedCount(counter.getDetailSyncedCount())
                .insertedCount(counter.getInsertedCount())
                .updatedCount(counter.getUpdatedCount())
                .tourApiCallCount(counter.getTourApiCallCount())
                .skippedDetailCount(counter.getSkippedDetailCount())
                .festivalPage(DEFAULT_PAGE)
                .festivalSize(validSize)
                .experiencePage(DEFAULT_PAGE)
                .experienceSize(validSize)
                .festivalStartPage(DEFAULT_PAGE)
                .festivalEndPage(lastFestivalPage)
                .experienceStartPage(DEFAULT_PAGE)
                .experienceEndPage(lastExperiencePage)
                .maxPages(validMaxPages)
                .detailLimit(validDetailLimit)
                .includeDetail(includeDetail)
                .automaticSync(true)
                .eventStartDate(validEventStartDate)
                .message(buildAllSyncMessage(includeDetail, counter))
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
                .festivalStartPage(0)
                .festivalEndPage(0)
                .experienceStartPage(0)
                .experienceEndPage(0)
                .maxPages(0)
                .detailLimit(1)
                .includeDetail(true)
                .automaticSync(false)
                .eventStartDate("")
                .message("contentId 기준으로 TourAPI 상세 데이터를 DB에 저장/갱신했습니다.")
                .build();
    }

    private int syncFestivalAllPages(
            int size,
            int maxPages,
            String eventStartDate,
            LocalDateTime syncedAt,
            SyncCounter counter,
            List<FestivalSummaryResponse> collector
    ) {
        if (!canCallListApi(counter)) {
            return 0;
        }

        SyncPageResult<FestivalSummaryResponse> firstPage = syncFestivalPage(
                DEFAULT_PAGE,
                size,
                eventStartDate,
                syncedAt,
                counter
        );

        collector.addAll(firstPage.items());

        int totalPages = calculateTotalPages(firstPage.totalCount(), size);
        int endPage = Math.min(totalPages, maxPages);
        int lastSyncedPage = DEFAULT_PAGE;

        for (int page = DEFAULT_PAGE + 1; page <= endPage; page++) {
            if (!canCallListApi(counter)) {
                break;
            }

            SyncPageResult<FestivalSummaryResponse> pageResult = syncFestivalPage(
                    page,
                    size,
                    eventStartDate,
                    syncedAt,
                    counter
            );

            collector.addAll(pageResult.items());
            lastSyncedPage = page;
        }

        return lastSyncedPage;
    }

    private int syncExperienceAllPages(
            String contentTypeId,
            int size,
            int maxPages,
            LocalDateTime syncedAt,
            SyncCounter counter,
            List<ExperienceSummaryResponse> collector
    ) {
        if (!canCallListApi(counter)) {
            return 0;
        }

        SyncPageResult<ExperienceSummaryResponse> firstPage = syncExperiencePage(
                DEFAULT_PAGE,
                size,
                contentTypeId,
                syncedAt,
                counter
        );

        collector.addAll(firstPage.items());

        int totalPages = calculateTotalPages(firstPage.totalCount(), size);
        int endPage = Math.min(totalPages, maxPages);
        int lastSyncedPage = DEFAULT_PAGE;

        for (int page = DEFAULT_PAGE + 1; page <= endPage; page++) {
            if (!canCallListApi(counter)) {
                break;
            }

            SyncPageResult<ExperienceSummaryResponse> pageResult = syncExperiencePage(
                    page,
                    size,
                    contentTypeId,
                    syncedAt,
                    counter
            );

            collector.addAll(pageResult.items());
            lastSyncedPage = page;
        }

        return lastSyncedPage;
    }

    private SyncPageResult<FestivalSummaryResponse> syncFestivalPage(
            int validFestivalPage,
            int validFestivalSize,
            String validEventStartDate,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
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

        List<FestivalSummaryResponse> festivalItems = festivalItemsOrEmpty(festivalListResponse);

        for (FestivalSummaryResponse item : festivalItems) {
            upsertFestivalSummary(item, syncedAt, counter);
        }

        int totalCount = parseTourApiTotalCount(festivalRawJson, festivalItems.size());

        return new SyncPageResult<>(festivalItems, totalCount);
    }

    private SyncPageResult<ExperienceSummaryResponse> syncExperiencePage(
            int validExperiencePage,
            int validExperienceSize,
            String contentTypeId,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        String experienceRawJson = tourApiClient.getExperienceListRaw(
                validExperiencePage,
                validExperienceSize,
                null,
                contentTypeId
        );
        counter.increaseTourApiCallCount();

        ExperienceListResponse experienceListResponse = festivalMapper.toExperienceListResponse(
                experienceRawJson,
                validExperiencePage,
                validExperienceSize
        );

        List<ExperienceSummaryResponse> experienceItems = experienceItemsOrEmpty(experienceListResponse);

        for (ExperienceSummaryResponse item : experienceItems) {
            upsertExperienceSummary(item, syncedAt, counter);
        }

        int totalCount = parseTourApiTotalCount(experienceRawJson, experienceItems.size());

        return new SyncPageResult<>(experienceItems, totalCount);
    }

    private void syncDetailIfRequested(
            List<FestivalSummaryResponse> festivalItems,
            List<ExperienceSummaryResponse> experienceItems,
            boolean includeDetail,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        if (includeDetail) {
            syncDetailsWithinLimit(
                    festivalItems,
                    experienceItems,
                    syncedAt,
                    counter,
                    MAX_DETAIL_SYNC_COUNT,
                    false
            );

            return;
        }

        counter.addSkippedDetailCount(festivalItems.size() + experienceItems.size());
    }

    private void syncDetailsWithinLimit(
            List<FestivalSummaryResponse> festivalItems,
            List<ExperienceSummaryResponse> experienceItems,
            LocalDateTime syncedAt,
            SyncCounter counter,
            int detailLimit,
            boolean skipAlreadyDetailed
    ) {
        int detailSyncCount = 0;

        for (FestivalSummaryResponse item : festivalItems) {
            if (detailSyncCount >= detailLimit || !canCallDetailApis(counter)) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            if (skipAlreadyDetailed && hasExistingDetail(item.getId())) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            syncDetail(item.getId(), syncedAt, counter);
            detailSyncCount++;
        }

        for (ExperienceSummaryResponse item : experienceItems) {
            if (detailSyncCount >= detailLimit || !canCallDetailApis(counter)) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            if (skipAlreadyDetailed && hasExistingDetail(item.getId())) {
                counter.increaseSkippedDetailCount();
                continue;
            }

            syncDetail(item.getId(), syncedAt, counter);
            detailSyncCount++;
        }
    }

    private boolean canCallListApi(SyncCounter counter) {
        return counter.getTourApiCallCount() + 1 <= MAX_TOUR_API_CALL_COUNT;
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

    private boolean hasExistingDetail(String contentId) {
        if (!hasText(contentId)) {
            return false;
        }

        return festivalContentRepository.findByContentId(contentId)
                .map(this::hasDetailContent)
                .orElse(false);
    }

    private boolean hasDetailContent(FestivalContent content) {
        if (content == null) {
            return false;
        }

        return hasText(content.getOverview())
                || hasText(content.getDescription())
                || hasMeaningfulJsonArray(content.getMainInfoJson());
    }

    private boolean hasMeaningfulJsonArray(String value) {
        if (!hasText(value)) {
            return false;
        }

        String text = value.trim();

        return !"[]".equals(text);
    }

    private List<FestivalSummaryResponse> festivalItemsOrEmpty(FestivalListResponse response) {
        if (response == null || response.getItems() == null) {
            return List.of();
        }

        return response.getItems();
    }

    private List<ExperienceSummaryResponse> experienceItemsOrEmpty(ExperienceListResponse response) {
        if (response == null || response.getItems() == null) {
            return List.of();
        }

        return response.getItems();
    }

    private int parseTourApiTotalCount(String rawJson, int fallbackCount) {
        if (!hasText(rawJson)) {
            return fallbackCount;
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);

            JsonNode responseBody = root.path("response").path("body");
            JsonNode body = responseBody.isMissingNode() || responseBody.isNull()
                    ? root.path("body")
                    : responseBody;

            int totalCount = body.path("totalCount").asInt(-1);

            if (totalCount >= 0) {
                return totalCount;
            }

            return fallbackCount;
        } catch (Exception e) {
            return fallbackCount;
        }
    }

    private int calculateTotalPages(int totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }

        return Math.max(1, (int) Math.ceil((double) totalCount / size));
    }

    private Map<String, Long> toCountMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();

        if (rows == null) {
            return result;
        }

        for (Object[] row : rows) {
            if (row == null || row.length < 2) {
                continue;
            }

            String key = normalizeMapKey(row[0]);
            long count = normalizeCount(row[1]);

            result.put(key, count);
        }

        return result;
    }

    private String normalizeMapKey(Object value) {
        if (value == null) {
            return "UNKNOWN";
        }

        String text = String.valueOf(value).trim();

        if (!hasText(text)) {
            return "UNKNOWN";
        }

        return text;
    }

    private long normalizeCount(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
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

    private String buildBulkMessage(boolean includeDetail, SyncCounter counter) {
        if (counter.getTourApiCallCount() >= MAX_TOUR_API_CALL_COUNT) {
            return "TourAPI 목록 데이터를 여러 페이지 범위로 DB에 저장/갱신했습니다. 서버 보호용 TourAPI 호출 제한 기준에 도달하여 일부 페이지 또는 상세 데이터 동기화가 중단될 수 있습니다.";
        }

        if (includeDetail) {
            if (counter.getDetailSyncedCount() > 0) {
                return "TourAPI 목록 데이터를 여러 페이지 범위로 DB에 저장/갱신했습니다. 상세 데이터는 트래픽 제한을 고려하여 일부만 보강했습니다.";
            }

            return "TourAPI 목록 데이터를 여러 페이지 범위로 DB에 저장/갱신했습니다. 상세 보강 대상이 없거나 제한 조건으로 인해 상세 데이터는 저장되지 않았습니다.";
        }

        return "TourAPI 목록 데이터를 여러 페이지 범위로 DB에 저장/갱신했습니다. 상세 데이터 보강은 contentId 단건 상세 동기화 API에서 수행할 수 있습니다.";
    }

    private String buildAllSyncMessage(boolean includeDetail, SyncCounter counter) {
        if (counter.getTourApiCallCount() >= MAX_TOUR_API_CALL_COUNT) {
            return "TourAPI totalCount 기준으로 전체 페이지 동기화를 시도했습니다. 서버 보호용 TourAPI 호출 제한 기준에 도달하여 일부 페이지 또는 상세 데이터 동기화가 중단될 수 있습니다.";
        }

        if (includeDetail) {
            return "TourAPI totalCount 기준으로 축제/관광지/문화시설/레포츠 데이터를 자동 동기화하고, detailLimit 범위 내에서 상세 데이터를 보강했습니다.";
        }

        return "TourAPI totalCount 기준으로 축제/관광지/문화시설/레포츠 목록 데이터를 자동 동기화했습니다. 상세 데이터 보강은 includeDetail=true로 실행할 수 있습니다.";
    }

    private int normalizePage(int page) {
        if (page < 1) {
            return DEFAULT_PAGE;
        }

        return page;
    }

    private int normalizeEndPage(int endPage, int startPage) {
        int validEndPage = normalizePage(endPage);

        if (validEndPage < startPage) {
            return startPage;
        }

        return validEndPage;
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

    private int normalizeMaxPages(int maxPages) {
        if (maxPages < 1) {
            return DEFAULT_MAX_AUTO_SYNC_PAGES;
        }

        if (maxPages > MAX_AUTO_SYNC_PAGES) {
            return MAX_AUTO_SYNC_PAGES;
        }

        return maxPages;
    }

    private int normalizeDetailLimit(int detailLimit) {
        if (detailLimit < 0) {
            return DEFAULT_DETAIL_SYNC_LIMIT;
        }

        if (detailLimit > MAX_DETAIL_SYNC_LIMIT) {
            return MAX_DETAIL_SYNC_LIMIT;
        }

        return detailLimit;
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

    private record SyncPageResult<T>(
            List<T> items,
            int totalCount
    ) {
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