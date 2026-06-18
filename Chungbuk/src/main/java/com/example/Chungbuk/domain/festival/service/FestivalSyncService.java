package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.FestivalSyncGroup;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceListResponse;
import com.example.Chungbuk.domain.festival.dto.response.ExperienceSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalDetailResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalListResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSummaryResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncGroupResultResponse;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class FestivalSyncService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 30;
    private static final int MAX_SIZE = 30;

    private static final int DEFAULT_MAX_PAGES = 50;
    private static final int MAX_MAX_PAGES = 50;

    /* 개발계정 일일 호출 한도 1,000회보다 여유를 둔 자동 동기화 상한 */
    private static final int DEFAULT_MAX_API_CALLS = 900;
    private static final int MAX_MAX_API_CALLS = 900;

    private static final int DEFAULT_DETAIL_LIMIT = 1000;
    private static final int MAX_DETAIL_LIMIT = 2000;

    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final String DEFAULT_EVENT_START_DATE = "20230101";

    private static final List<String> EXPERIENCE_CONTENT_TYPE_IDS =
            List.of("12", "14", "28");

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter TOUR_API_MODIFIED_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;
    private final FestivalContentRepository festivalContentRepository;
    private final PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean syncRunning = new AtomicBoolean(false);

    @Transactional(readOnly = true)
    public FestivalSyncStatusResponse getFestivalSyncStatus() {
        long totalCount = festivalContentRepository.countByActiveTrue();

        long festivalCount = festivalContentRepository
                .countByContentTypeIdAndActiveTrue("15");

        long touristSpotCount = festivalContentRepository
                .countByContentTypeIdAndActiveTrue("12");

        long cultureCount = festivalContentRepository
                .countByContentTypeIdAndActiveTrue("14");

        long leportsCount = festivalContentRepository
                .countByContentTypeIdAndActiveTrue("28");

        long detailSyncedCount = festivalContentRepository
                .countDetailSyncedContents();

        LocalDateTime lastSyncedAt = festivalContentRepository
                .findLatestSyncedAt();

        return FestivalSyncStatusResponse.builder()
                .totalCount(totalCount)
                .festivalCount(festivalCount)
                .touristSpotCount(touristSpotCount)
                .cultureCount(cultureCount)
                .leportsCount(leportsCount)
                .detailSyncedCount(detailSyncedCount)
                .lastSyncedAt(lastSyncedAt)
                .empty(totalCount == 0)
                .contentTypeCounts(toCountMap(
                        festivalContentRepository
                                .countActiveContentsByContentTypeId()
                ))
                .categoryCounts(toCountMap(
                        festivalContentRepository
                                .countActiveContentsByCategory()
                ))
                .build();
    }

    /* 새벽 스케줄러 전용 진입점 */
    public FestivalSyncResultResponse refreshFestivalContents() {
        return runListRefresh(
                new SyncRequest(
                        DEFAULT_SIZE,
                        DEFAULT_MAX_PAGES,
                        DEFAULT_EVENT_START_DATE,
                        true,
                        DEFAULT_DETAIL_LIMIT,
                        DEFAULT_MAX_API_CALLS,
                        true,
                        false,
                        true
                )
        );
    }

    /*
     * FestivalController의 기존 /sync/bootstrap API 호환용 진입점.
     * 최초 적재는 목록 누락을 삭제로 처리하지 않고, 모든 상세를 다시 확인한다.
     */
    public FestivalSyncResultResponse bootstrapFestivalContents(
            int size,
            int maxPages,
            int maxApiCalls,
            boolean includeImages
    ) {
        return runListRefresh(
                new SyncRequest(
                        normalizeSize(size),
                        normalizeMaxPages(maxPages),
                        DEFAULT_EVENT_START_DATE,
                        includeImages,
                        DEFAULT_DETAIL_LIMIT,
                        normalizeMaxApiCalls(maxApiCalls),
                        false,
                        true,
                        false
                )
        );
    }

    /*
     * FestivalController의 기존 /sync/refresh API 호환용 진입점.
     * 수동 refresh는 자동 스케줄 실행과 같은 변경 감지·안전 비활성화 흐름을 사용한다.
     */
    public FestivalSyncResultResponse refreshFestivalContents(
            int size,
            int maxPages,
            int maxApiCalls,
            boolean includeImages
    ) {
        return runListRefresh(
                new SyncRequest(
                        normalizeSize(size),
                        normalizeMaxPages(maxPages),
                        DEFAULT_EVENT_START_DATE,
                        includeImages,
                        DEFAULT_DETAIL_LIMIT,
                        normalizeMaxApiCalls(maxApiCalls),
                        true,
                        false,
                        false
                )
        );
    }

    /* 기존 Swagger 단일 페이지 동기화 API 호환용 */
    public FestivalSyncResultResponse syncFestivalContents(
            int festivalPage,
            int festivalSize,
            int experiencePage,
            int experienceSize,
            String eventStartDate,
            boolean includeDetail
    ) {
        int size = Math.max(
                normalizeSize(festivalSize),
                normalizeSize(experienceSize)
        );

        return runListRefresh(
                new SyncRequest(
                        size,
                        1,
                        normalizeEventStartDate(eventStartDate),
                        includeDetail,
                        includeDetail ? size * 4 : 0,
                        DEFAULT_MAX_API_CALLS,
                        false,
                        false,
                        false
                )
        );
    }

    public FestivalSyncResultResponse syncFestivalContentsAll(
            int size,
            int maxPages,
            String eventStartDate,
            boolean includeDetail,
            int detailLimit
    ) {
        return syncFestivalContentsAll(
                size,
                maxPages,
                eventStartDate,
                includeDetail,
                detailLimit,
                false
        );
    }

    public FestivalSyncResultResponse syncFestivalContentsAll(
            int size,
            int maxPages,
            String eventStartDate,
            boolean includeDetail,
            int detailLimit,
            boolean deactivateMissing
    ) {
        return runListRefresh(
                new SyncRequest(
                        normalizeSize(size),
                        normalizeMaxPages(maxPages),
                        normalizeEventStartDate(eventStartDate),
                        includeDetail,
                        normalizeDetailLimit(detailLimit),
                        DEFAULT_MAX_API_CALLS,
                        deactivateMissing,
                        false,
                        false
                )
        );
    }

    /* 기존 /sync/sequential API는 새 공정 순환 refresh 로직을 사용한다. */
    public FestivalSyncResultResponse syncFestivalContentsSequential(
            int size,
            int maxPages,
            String eventStartDate,
            int detailLimitPerGroup,
            int maxApiCalls,
            boolean onlyMissing
    ) {
        return runListRefresh(
                new SyncRequest(
                        normalizeSize(size),
                        normalizeMaxPages(maxPages),
                        normalizeEventStartDate(eventStartDate),
                        true,
                        normalizeDetailLimit(detailLimitPerGroup),
                        normalizeMaxApiCalls(maxApiCalls),
                        true,
                        !onlyMissing,
                        true
                )
        );
    }

    /* 기존 카드 보강 API는 상세 미완료 대상 보강으로 통합한다. */
    public FestivalSyncResultResponse syncFestivalCardInfo(
            int limit,
            int maxApiCalls,
            boolean onlyMissing
    ) {
        return syncFestivalDetails(limit, maxApiCalls, onlyMissing);
    }

    public FestivalSyncResultResponse syncFestivalDetails(
            int limit,
            int maxApiCalls,
            boolean onlyMissing
    ) {
        if (!syncRunning.compareAndSet(false, true)) {
            return emptySyncResponse(
                    "축제·체험 동기화가 이미 실행 중입니다. 기존 요청이 끝난 뒤 다시 실행하세요."
            );
        }

        try {
            LocalDateTime syncedAt = LocalDateTime.now();
            SyncCounter counter = new SyncCounter();
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                    createGroupWorkMap();

            int validLimit = normalizeDetailLimit(limit);
            int validMaxApiCalls = normalizeMaxApiCalls(maxApiCalls);

            addStoredDetailTargets(
                    groupWorkMap,
                    onlyMissing,
                    validLimit
            );

            processDetailTasksRoundRobin(
                    groupWorkMap,
                    validMaxApiCalls,
                    syncedAt,
                    counter
            );

            return buildResponse(
                    counter,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    validLimit,
                    true,
                    false,
                    false,
                    true,
                    "",
                    buildDetailOnlyMessage(counter),
                    buildGroupResults(groupWorkMap)
            );
        } finally {
            syncRunning.set(false);
        }
    }

    /* FestivalController의 includeImages 파라미터 호환용 진입점. */
    public FestivalSyncResultResponse syncFestivalContentDetail(
            String contentId,
            boolean includeImages
    ) {
        /*
         * 현재 단건 재동기화는 상세 공통·유형별·이미지를 함께 정합성 있게 갱신한다.
         * includeImages=false로 호출해도 기존 상세 이미지가 남아 있을 수 있으므로
         * 단건 API에서는 항상 전체 상세 갱신을 수행한다.
         */
        return syncFestivalContentDetail(contentId);
    }

    public FestivalSyncResultResponse syncFestivalContentDetail(
            String contentId
    ) {
        if (!hasText(contentId)) {
            return emptySyncResponse("contentId를 입력하세요.");
        }

        if (!syncRunning.compareAndSet(false, true)) {
            return emptySyncResponse(
                    "축제·체험 동기화가 이미 실행 중입니다. 기존 요청이 끝난 뒤 다시 실행하세요."
            );
        }

        try {
            FestivalContent content = festivalContentRepository
                    .findByContentId(contentId)
                    .orElse(null);

            if (content == null) {
                return emptySyncResponse(
                        "DB에 저장된 콘텐츠가 없습니다. 목록 동기화 후 다시 실행하세요."
                );
            }

            LocalDateTime syncedAt = LocalDateTime.now();
            SyncCounter counter = new SyncCounter();
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                    createGroupWorkMap();

            FestivalSyncGroup group = resolveGroup(content);
            groupWorkMap.get(group).addDetailTask(
                    new DetailTask(
                            contentId,
                            content.getContentTypeId(),
                            content.getSourceUpdatedAt(),
                            DetailTaskType.FULL
                    )
            );

            processDetailTasksRoundRobin(
                    groupWorkMap,
                    3,
                    syncedAt,
                    counter
            );

            return buildResponse(
                    counter,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    1,
                    true,
                    false,
                    false,
                    true,
                    "",
                    buildDetailOnlyMessage(counter),
                    buildGroupResults(groupWorkMap)
            );
        } finally {
            syncRunning.set(false);
        }
    }

    private FestivalSyncResultResponse runListRefresh(
            SyncRequest request
    ) {
        if (!syncRunning.compareAndSet(false, true)) {
            return emptySyncResponse(
                    "축제·체험 동기화가 이미 실행 중입니다. 기존 요청이 끝난 뒤 다시 실행하세요."
            );
        }

        try {
            LocalDateTime syncedAt = LocalDateTime.now();
            SyncCounter counter = new SyncCounter();
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                    createGroupWorkMap();

            FetchResult<FestivalSummaryResponse> eventFetchResult =
                    fetchFestivalPages(
                            request.size(),
                            request.maxPages(),
                            request.eventStartDate(),
                            request.maxApiCalls(),
                            counter
                    );

            GroupSyncWork festivalWork = groupWorkMap.get(
                    FestivalSyncGroup.FESTIVAL
            );
            festivalWork.addApiCallCount(eventFetchResult.apiCallCount());

            for (SourceItem<FestivalSummaryResponse> sourceItem
                    : eventFetchResult.items()) {
                FestivalSyncGroup group = resolveFestivalGroup(
                        sourceItem.item()
                );

                GroupSyncWork work = groupWorkMap.get(group);
                work.increaseTargetCount();

                SummarySaveResult saveResult = saveFestivalSummary(
                        sourceItem,
                        syncedAt,
                        request.includeDetail(),
                        request.forceAllDetails(),
                        counter
                );

                if (saveResult.saved()) {
                    work.increaseMainSyncedCount();
                }

                work.addDetailTask(saveResult.detailTask());
            }

            Map<FestivalSyncGroup, FetchResult<ExperienceSummaryResponse>>
                    experienceFetchResults = new LinkedHashMap<>();

            for (FestivalSyncGroup group : List.of(
                    FestivalSyncGroup.TOURIST_ATTRACTION,
                    FestivalSyncGroup.CULTURAL_FACILITY,
                    FestivalSyncGroup.LEPORTS
            )) {
                FetchResult<ExperienceSummaryResponse> fetchResult =
                        fetchExperiencePages(
                                group.getContentTypeId(),
                                request.size(),
                                request.maxPages(),
                                request.maxApiCalls(),
                                counter
                        );

                experienceFetchResults.put(group, fetchResult);

                GroupSyncWork work = groupWorkMap.get(group);
                work.addApiCallCount(fetchResult.apiCallCount());

                for (SourceItem<ExperienceSummaryResponse> sourceItem
                        : fetchResult.items()) {
                    work.increaseTargetCount();

                    SummarySaveResult saveResult = saveExperienceSummary(
                            sourceItem,
                            syncedAt,
                            request.includeDetail(),
                            request.forceAllDetails(),
                            counter
                    );

                    if (saveResult.saved()) {
                        work.increaseMainSyncedCount();
                    }

                    work.addDetailTask(saveResult.detailTask());
                }
            }

            boolean allListsSafe = eventFetchResult.isSafeForDeactivation()
                    && experienceFetchResults.values().stream()
                    .allMatch(FetchResult::isSafeForDeactivation);

            boolean deactivationApplied = false;
            boolean deactivationDeferred = false;

            if (request.deactivateMissing()) {
                if (allListsSafe) {
                    deactivateMissingContents(
                            "15",
                            eventFetchResult.contentIds(),
                            counter
                    );

                    for (FestivalSyncGroup group : List.of(
                            FestivalSyncGroup.TOURIST_ATTRACTION,
                            FestivalSyncGroup.CULTURAL_FACILITY,
                            FestivalSyncGroup.LEPORTS
                    )) {
                        FetchResult<ExperienceSummaryResponse> fetchResult =
                                experienceFetchResults.get(group);

                        deactivateMissingContents(
                                group.getContentTypeId(),
                                fetchResult.contentIds(),
                                counter
                        );
                    }

                    deactivationApplied = true;
                } else {
                    deactivationDeferred = true;
                }
            }

            if (request.includeDetail()) {
                processDetailTasksRoundRobin(
                        groupWorkMap,
                        request.maxApiCalls(),
                        syncedAt,
                        counter
                );
            }

            int maxExperiencePage = experienceFetchResults.values().stream()
                    .mapToInt(FetchResult::lastPage)
                    .max()
                    .orElse(0);

            return buildResponse(
                    counter,
                    DEFAULT_PAGE,
                    request.size(),
                    DEFAULT_PAGE,
                    request.size(),
                    DEFAULT_PAGE,
                    eventFetchResult.lastPage(),
                    DEFAULT_PAGE,
                    maxExperiencePage,
                    request.maxPages(),
                    request.detailLimit(),
                    request.includeDetail(),
                    request.automatic(),
                    deactivationApplied,
                    true,
                    request.eventStartDate(),
                    buildRefreshMessage(
                            request,
                            allListsSafe,
                            deactivationApplied,
                            deactivationDeferred,
                            counter
                    ),
                    buildGroupResults(groupWorkMap)
            );
        } finally {
            syncRunning.set(false);
        }
    }

    private FetchResult<FestivalSummaryResponse> fetchFestivalPages(
            int size,
            int maxPages,
            String eventStartDate,
            int maxApiCalls,
            SyncCounter counter
    ) {
        List<SourceItem<FestivalSummaryResponse>> items = new ArrayList<>();
        Set<String> contentIds = new LinkedHashSet<>();

        int totalPages = 1;
        int lastPage = 0;
        int sourceTotalCount = -1;
        int apiCallCount = 0;
        boolean completed = true;

        for (int page = 1; page <= totalPages; page++) {
            if (page > maxPages || !canCallApi(counter, maxApiCalls, 1)) {
                completed = false;
                break;
            }

            ApiResponse response = requestFestivalListRaw(
                    page,
                    size,
                    eventStartDate,
                    counter
            );
            apiCallCount++;

            if (!response.success()) {
                completed = false;
                break;
            }

            int pageTotalCount = readTourApiTotalCount(response.rawJson());

            if (pageTotalCount < 0) {
                completed = false;
                break;
            }

            if (sourceTotalCount < 0) {
                sourceTotalCount = pageTotalCount;
            } else if (sourceTotalCount != pageTotalCount) {
                completed = false;
                break;
            }

            FestivalListResponse mappedResponse =
                    festivalMapper.toFestivalListResponse(
                            response.rawJson(),
                            page,
                            size
                    );

            Map<String, LocalDateTime> modifiedTimeMap =
                    extractModifiedTimeMap(response.rawJson());

            List<FestivalSummaryResponse> pageItems =
                    mappedResponse.getItems() == null
                            ? List.of()
                            : mappedResponse.getItems();

            for (FestivalSummaryResponse item : pageItems) {
                if (item == null || !hasText(item.getId())) {
                    continue;
                }

                items.add(new SourceItem(
                        item,
                        modifiedTimeMap.get(item.getId())
                ));
                contentIds.add(item.getId());
            }

            totalPages = calculateTotalPages(pageTotalCount, size);
            lastPage = page;
        }

        if (sourceTotalCount < 0 || contentIds.size() != sourceTotalCount) {
            completed = false;
        }

        return new FetchResult<>(
                items,
                contentIds,
                lastPage,
                sourceTotalCount,
                apiCallCount,
                completed
        );
    }

    private FetchResult<ExperienceSummaryResponse> fetchExperiencePages(
            String contentTypeId,
            int size,
            int maxPages,
            int maxApiCalls,
            SyncCounter counter
    ) {
        List<SourceItem<ExperienceSummaryResponse>> items = new ArrayList<>();
        Set<String> contentIds = new LinkedHashSet<>();

        int totalPages = 1;
        int lastPage = 0;
        int sourceTotalCount = -1;
        int apiCallCount = 0;
        boolean completed = true;

        for (int page = 1; page <= totalPages; page++) {
            if (page > maxPages || !canCallApi(counter, maxApiCalls, 1)) {
                completed = false;
                break;
            }

            ApiResponse response = requestExperienceListRaw(
                    page,
                    size,
                    contentTypeId,
                    counter
            );
            apiCallCount++;

            if (!response.success()) {
                completed = false;
                break;
            }

            int pageTotalCount = readTourApiTotalCount(response.rawJson());

            if (pageTotalCount < 0) {
                completed = false;
                break;
            }

            if (sourceTotalCount < 0) {
                sourceTotalCount = pageTotalCount;
            } else if (sourceTotalCount != pageTotalCount) {
                completed = false;
                break;
            }

            ExperienceListResponse mappedResponse =
                    festivalMapper.toExperienceListResponse(
                            response.rawJson(),
                            page,
                            size
                    );

            Map<String, LocalDateTime> modifiedTimeMap =
                    extractModifiedTimeMap(response.rawJson());

            List<ExperienceSummaryResponse> pageItems =
                    mappedResponse.getItems() == null
                            ? List.of()
                            : mappedResponse.getItems();

            for (ExperienceSummaryResponse item : pageItems) {
                if (item == null || !hasText(item.getId())) {
                    continue;
                }

                items.add(new SourceItem(
                        item,
                        modifiedTimeMap.get(item.getId())
                ));
                contentIds.add(item.getId());
            }

            totalPages = calculateTotalPages(pageTotalCount, size);
            lastPage = page;
        }

        if (sourceTotalCount < 0 || contentIds.size() != sourceTotalCount) {
            completed = false;
        }

        return new FetchResult<>(
                items,
                contentIds,
                lastPage,
                sourceTotalCount,
                apiCallCount,
                completed
        );
    }

    private SummarySaveResult saveFestivalSummary(
            SourceItem<FestivalSummaryResponse> sourceItem,
            LocalDateTime syncedAt,
            boolean includeDetail,
            boolean forceAllDetails,
            SyncCounter counter
    ) {
        FestivalSummaryResponse item = sourceItem.item();

        if (item == null || !hasText(item.getId())) {
            return SummarySaveResult.empty();
        }

        FestivalContent existingContent = festivalContentRepository
                .findByContentId(item.getId())
                .orElse(null);

        boolean inserted = existingContent == null;
        boolean hadDetailBefore = hasDetailContent(existingContent);

        FestivalContent content = inserted
                ? FestivalContent.builder()
                .contentId(item.getId())
                .active(true)
                .build()
                : existingContent;

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
                syncedAt,
                sourceItem.sourceUpdatedAt()
        );

        if (!inserted && hadDetailBefore) {
            content.initializeDailyRefreshBaseline(
                    sourceItem.sourceUpdatedAt()
            );
        }

        festivalContentRepository.save(content);

        if (inserted) {
            counter.increaseInsertedCount();
        } else {
            counter.increaseUpdatedCount();
        }
        counter.increaseFestivalSavedCount();

        return new SummarySaveResult(
                true,
                createDetailTask(
                        content,
                        includeDetail,
                        forceAllDetails,
                        sourceItem.sourceUpdatedAt()
                )
        );
    }

    private SummarySaveResult saveExperienceSummary(
            SourceItem<ExperienceSummaryResponse> sourceItem,
            LocalDateTime syncedAt,
            boolean includeDetail,
            boolean forceAllDetails,
            SyncCounter counter
    ) {
        ExperienceSummaryResponse item = sourceItem.item();

        if (item == null || !hasText(item.getId())) {
            return SummarySaveResult.empty();
        }

        FestivalContent existingContent = festivalContentRepository
                .findByContentId(item.getId())
                .orElse(null);

        boolean inserted = existingContent == null;
        boolean hadDetailBefore = hasDetailContent(existingContent);

        FestivalContent content = inserted
                ? FestivalContent.builder()
                .contentId(item.getId())
                .active(true)
                .build()
                : existingContent;

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
                syncedAt,
                sourceItem.sourceUpdatedAt()
        );

        if (!inserted && hadDetailBefore) {
            content.initializeDailyRefreshBaseline(
                    sourceItem.sourceUpdatedAt()
            );
        }

        festivalContentRepository.save(content);

        if (inserted) {
            counter.increaseInsertedCount();
        } else {
            counter.increaseUpdatedCount();
        }
        counter.increaseExperienceSavedCount();

        return new SummarySaveResult(
                true,
                createDetailTask(
                        content,
                        includeDetail,
                        forceAllDetails,
                        sourceItem.sourceUpdatedAt()
                )
        );
    }

    private DetailTask createDetailTask(
            FestivalContent content,
            boolean includeDetail,
            boolean forceAllDetails,
            LocalDateTime sourceUpdatedAt
    ) {
        if (!includeDetail || content == null || !hasText(content.getContentId())) {
            return null;
        }

        if (forceAllDetails || !hasDetailContent(content)) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    DetailTaskType.FULL
            );
        }

        if (isSourceModifiedAfterDetail(content, sourceUpdatedAt)) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    DetailTaskType.FULL
            );
        }

        if (!Boolean.TRUE.equals(content.getImageSyncCompleted())) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    DetailTaskType.IMAGE_ONLY
            );
        }

        return null;
    }

    private void addStoredDetailTargets(
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap,
            boolean onlyMissing,
            int limit
    ) {
        List<FestivalContent> contents = festivalContentRepository
                .findAllByActiveTrueAndContentTypeIdIn(
                        List.of("12", "14", "15", "28")
                );

        int addedCount = 0;

        for (FestivalContent content : contents) {
            if (addedCount >= limit) {
                break;
            }

            DetailTask task;

            if (!onlyMissing) {
                task = new DetailTask(
                        content.getContentId(),
                        content.getContentTypeId(),
                        content.getSourceUpdatedAt(),
                        DetailTaskType.FULL
                );
            } else if (!hasDetailContent(content)) {
                task = new DetailTask(
                        content.getContentId(),
                        content.getContentTypeId(),
                        content.getSourceUpdatedAt(),
                        DetailTaskType.FULL
                );
            } else if (!Boolean.TRUE.equals(content.getImageSyncCompleted())) {
                task = new DetailTask(
                        content.getContentId(),
                        content.getContentTypeId(),
                        content.getSourceUpdatedAt(),
                        DetailTaskType.IMAGE_ONLY
                );
            } else {
                continue;
            }

            FestivalSyncGroup group = resolveGroup(content);
            groupWorkMap.get(group).addDetailTask(task);
            addedCount++;
        }
    }

    private void processDetailTasksRoundRobin(
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap,
            int maxApiCalls,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        Map<FestivalSyncGroup, Integer> nextIndexMap = new HashMap<>();

        for (FestivalSyncGroup group : FestivalSyncGroup.orderedGroups()) {
            nextIndexMap.put(group, 0);
        }

        while (true) {
            boolean hasRemainingTask = false;

            for (FestivalSyncGroup group : FestivalSyncGroup.orderedGroups()) {
                GroupSyncWork work = groupWorkMap.get(group);
                int nextIndex = nextIndexMap.get(group);

                if (nextIndex >= work.detailTasks().size()) {
                    continue;
                }

                hasRemainingTask = true;

                DetailTask task = work.detailTasks().get(nextIndex);
                int requiredApiCalls = task.type().requiredApiCalls();

                if (!canCallApi(counter, maxApiCalls, requiredApiCalls)) {
                    markRemainingTasksSkipped(
                            groupWorkMap,
                            nextIndexMap,
                            counter
                    );
                    return;
                }

                CounterSnapshot before = CounterSnapshot.from(counter);
                DetailSyncOutcome outcome = syncDetailSafely(
                        task,
                        syncedAt,
                        counter
                );
                CounterSnapshot after = CounterSnapshot.from(counter);

                work.addApiCallCount(
                        after.tourApiCallCount() - before.tourApiCallCount()
                );
                nextIndexMap.put(group, nextIndex + 1);

                switch (outcome) {
                    case SUCCESS -> work.increaseDetailSyncedCount();
                    case COMMON_ONLY -> {
                        work.increaseDetailSyncedCount();
                        work.increaseDetailEmptyCount();
                        counter.increaseDetailEmptyCount();
                    }
                    case EMPTY -> {
                        work.increaseDetailEmptyCount();
                        counter.increaseDetailEmptyCount();
                    }
                    case PARTIAL -> {
                        work.increaseDetailSyncedCount();
                        work.increaseFailedCount();
                    }
                    case FAILURE -> work.increaseFailedCount();
                }
            }

            if (!hasRemainingTask) {
                return;
            }
        }
    }

    private void markRemainingTasksSkipped(
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap,
            Map<FestivalSyncGroup, Integer> nextIndexMap,
            SyncCounter counter
    ) {
        for (FestivalSyncGroup group : FestivalSyncGroup.orderedGroups()) {
            GroupSyncWork work = groupWorkMap.get(group);
            int nextIndex = nextIndexMap.get(group);
            int remainingCount = Math.max(
                    work.detailTasks().size() - nextIndex,
                    0
            );

            if (remainingCount == 0) {
                continue;
            }

            work.addSkippedCount(remainingCount);
            counter.addSkippedDetailCount(remainingCount);
        }
    }

    private DetailSyncOutcome syncDetailSafely(
            DetailTask task,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        try {
            if (task.type() == DetailTaskType.IMAGE_ONLY) {
                return syncImageOnly(task, syncedAt, counter);
            }

            return syncFullDetail(task, syncedAt, counter);
        } catch (DataAccessException e) {
            counter.increaseFailedRequestCount();
            return DetailSyncOutcome.FAILURE;
        } catch (RuntimeException e) {
            counter.increaseFailedRequestCount();
            return DetailSyncOutcome.FAILURE;
        }
    }

    private DetailSyncOutcome syncFullDetail(
            DetailTask task,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        ApiResponse commonResponse = requestDetailCommonRaw(
                task.contentId(),
                counter
        );

        if (!commonResponse.success()) {
            return DetailSyncOutcome.FAILURE;
        }

        if (!festivalMapper.hasDetailItem(commonResponse.rawJson())) {
            markDetailCommonUnavailable(
                    task.contentId(),
                    task.sourceUpdatedAt(),
                    syncedAt,
                    counter
            );
            return DetailSyncOutcome.EMPTY;
        }

        FestivalContent existingContent = festivalContentRepository
                .findByContentId(task.contentId())
                .orElse(null);

        String contentTypeId = firstNonBlank(
                task.contentTypeId(),
                existingContent == null
                        ? ""
                        : existingContent.getContentTypeId(),
                festivalMapper.extractContentTypeId(commonResponse.rawJson()),
                DEFAULT_FESTIVAL_CONTENT_TYPE_ID
        );

        ApiResponse introResponse = requestDetailIntroRaw(
                task.contentId(),
                contentTypeId,
                counter
        );

        if (!introResponse.success()) {
            return DetailSyncOutcome.FAILURE;
        }

        ApiResponse imageResponse = requestDetailImageRaw(
                task.contentId(),
                counter
        );

        boolean imageSynced = imageResponse.success();
        boolean hasIntroItem = festivalMapper.hasDetailItem(
                introResponse.rawJson()
        );

        FestivalDetailResponse detailResponse =
                festivalMapper.toFestivalDetailResponse(
                        commonResponse.rawJson(),
                        hasIntroItem ? introResponse.rawJson() : "",
                        imageSynced ? imageResponse.rawJson() : "",
                        "",
                        task.contentId()
                );

        saveDetail(
                detailResponse,
                task.sourceUpdatedAt(),
                imageSynced,
                syncedAt,
                counter
        );

        if (!imageSynced) {
            return DetailSyncOutcome.PARTIAL;
        }

        return hasIntroItem
                ? DetailSyncOutcome.SUCCESS
                : DetailSyncOutcome.COMMON_ONLY;
    }

    private DetailSyncOutcome syncImageOnly(
            DetailTask task,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        ApiResponse imageResponse = requestDetailImageRaw(
                task.contentId(),
                counter
        );

        if (!imageResponse.success()) {
            return DetailSyncOutcome.FAILURE;
        }

        saveImageOnly(
                task.contentId(),
                imageResponse.rawJson(),
                syncedAt,
                counter
        );

        return DetailSyncOutcome.SUCCESS;
    }

    private void saveDetail(
            FestivalDetailResponse detail,
            LocalDateTime sourceUpdatedAt,
            boolean imageSynced,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        TransactionTemplate transactionTemplate =
                createRequiredNewTransactionTemplate();

        transactionTemplate.executeWithoutResult(status -> {
            FestivalContent content = festivalContentRepository
                    .findByContentId(detail.getId())
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
                    sourceUpdatedAt,
                    sourceUpdatedAt,
                    imageSynced
            );

            festivalContentRepository.save(content);
            counter.increaseDetailSyncedCount();
        });
    }

    private void saveImageOnly(
            String contentId,
            String imageRawJson,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        TransactionTemplate transactionTemplate =
                createRequiredNewTransactionTemplate();

        transactionTemplate.executeWithoutResult(status -> {
            FestivalContent content = festivalContentRepository
                    .findByContentId(contentId)
                    .orElse(null);

            if (content == null) {
                counter.increaseFailedRequestCount();
                return;
            }

            FestivalDetailResponse mapped =
                    festivalMapper.toFestivalDetailResponse(
                            "",
                            "",
                            imageRawJson,
                            "",
                            contentId
                    );

            content.updateImageInfo(
                    toJson(mapped.getImageUrls()),
                    syncedAt
            );

            festivalContentRepository.save(content);
            counter.increaseUpdatedCount();
            counter.increaseDetailSyncedCount();
        });
    }

    private void markDetailCommonUnavailable(
            String contentId,
            LocalDateTime sourceUpdatedAt,
            LocalDateTime syncedAt,
            SyncCounter counter
    ) {
        TransactionTemplate transactionTemplate =
                createRequiredNewTransactionTemplate();

        transactionTemplate.executeWithoutResult(status -> {
            FestivalContent content = festivalContentRepository
                    .findByContentId(contentId)
                    .orElse(null);

            if (content == null) {
                counter.increaseFailedRequestCount();
                return;
            }

            content.markDetailCommonUnavailable(
                    syncedAt,
                    sourceUpdatedAt
            );

            festivalContentRepository.save(content);
            counter.increaseUpdatedCount();
        });
    }

    private TransactionTemplate createRequiredNewTransactionTemplate() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
        transactionTemplate.setTimeout(20);

        return transactionTemplate;
    }

    private void deactivateMissingContents(
            String contentTypeId,
            Collection<String> currentContentIds,
            SyncCounter counter
    ) {
        Set<String> currentIdSet = currentContentIds == null
                ? Set.of()
                : new HashSet<>(currentContentIds);

        List<FestivalContent> activeContents = festivalContentRepository
                .findAllByActiveTrueAndContentTypeIdIn(
                        List.of(contentTypeId)
                );

        int deactivatedCount = 0;

        for (FestivalContent content : activeContents) {
            if (currentIdSet.contains(content.getContentId())) {
                continue;
            }

            content.deactivate();
            deactivatedCount++;
        }

        if (deactivatedCount > 0) {
            festivalContentRepository.saveAll(activeContents);
            counter.addDeactivatedCount(deactivatedCount);
        }
    }

    private ApiResponse requestFestivalListRaw(
            int page,
            int size,
            String eventStartDate,
            SyncCounter counter
    ) {
        counter.increaseTourApiCallCount();

        try {
            String rawJson = tourApiClient.getFestivalListRaw(
                    page,
                    size,
                    eventStartDate,
                    null
            );

            return validateApiResponse(rawJson, counter);
        } catch (RestClientException e) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }
    }

    private ApiResponse requestExperienceListRaw(
            int page,
            int size,
            String contentTypeId,
            SyncCounter counter
    ) {
        counter.increaseTourApiCallCount();

        try {
            String rawJson = tourApiClient.getExperienceListRaw(
                    page,
                    size,
                    null,
                    contentTypeId
            );

            return validateApiResponse(rawJson, counter);
        } catch (RestClientException e) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }
    }

    private ApiResponse requestDetailCommonRaw(
            String contentId,
            SyncCounter counter
    ) {
        counter.increaseTourApiCallCount();

        try {
            return validateApiResponse(
                    tourApiClient.getFestivalDetailCommonRaw(contentId),
                    counter
            );
        } catch (RestClientException e) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }
    }

    private ApiResponse requestDetailIntroRaw(
            String contentId,
            String contentTypeId,
            SyncCounter counter
    ) {
        counter.increaseTourApiCallCount();

        try {
            return validateApiResponse(
                    tourApiClient.getFestivalDetailIntroRaw(
                            contentId,
                            contentTypeId
                    ),
                    counter
            );
        } catch (RestClientException e) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }
    }

    private ApiResponse requestDetailImageRaw(
            String contentId,
            SyncCounter counter
    ) {
        counter.increaseTourApiCallCount();

        try {
            return validateApiResponse(
                    tourApiClient.getFestivalDetailImageRaw(contentId),
                    counter
            );
        } catch (RestClientException e) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }
    }

    private ApiResponse validateApiResponse(
            String rawJson,
            SyncCounter counter
    ) {
        if (!hasText(rawJson) || !isTourApiSuccess(rawJson)) {
            counter.increaseFailedRequestCount();
            return ApiResponse.failed();
        }

        return ApiResponse.success(rawJson);
    }

    private boolean isTourApiSuccess(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            String resultCode = root.path("response")
                    .path("header")
                    .path("resultCode")
                    .asText("");

            return "0000".equals(resultCode);
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, LocalDateTime> extractModifiedTimeMap(
            String rawJson
    ) {
        Map<String, LocalDateTime> result = new HashMap<>();

        try {
            JsonNode body = getBody(rawJson);
            JsonNode itemNode = body.path("items").path("item");

            for (JsonNode item : toItemList(itemNode)) {
                String contentId = item.path("contentid").asText("").trim();
                LocalDateTime modifiedAt = parseModifiedTime(
                        item.path("modifiedtime").asText("")
                );

                if (hasText(contentId) && modifiedAt != null) {
                    result.put(contentId, modifiedAt);
                }
            }
        } catch (Exception ignored) {
            return Map.of();
        }

        return result;
    }

    private JsonNode getBody(String rawJson) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawJson);
        JsonNode responseBody = root.path("response").path("body");

        if (!responseBody.isMissingNode() && !responseBody.isNull()) {
            return responseBody;
        }

        return root.path("body");
    }

    private List<JsonNode> toItemList(JsonNode itemNode) {
        if (itemNode == null
                || itemNode.isMissingNode()
                || itemNode.isNull()) {
            return List.of();
        }

        if (itemNode.isArray()) {
            List<JsonNode> items = new ArrayList<>();
            itemNode.forEach(items::add);
            return items;
        }

        if (itemNode.isObject()) {
            return List.of(itemNode);
        }

        return List.of();
    }

    private int readTourApiTotalCount(String rawJson) {
        try {
            JsonNode totalCountNode = getBody(rawJson).path("totalCount");

            if (totalCountNode.isMissingNode() || totalCountNode.isNull()) {
                return -1;
            }

            return totalCountNode.asInt(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    private int calculateTotalPages(int totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }

        return Math.max(
                1,
                (int) Math.ceil((double) totalCount / size)
        );
    }

    private Map<FestivalSyncGroup, GroupSyncWork> createGroupWorkMap() {
        Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                new LinkedHashMap<>();

        for (FestivalSyncGroup group : FestivalSyncGroup.orderedGroups()) {
            groupWorkMap.put(group, new GroupSyncWork());
        }

        return groupWorkMap;
    }

    private FestivalSyncGroup resolveFestivalGroup(
            FestivalSummaryResponse item
    ) {
        if (item == null) {
            return FestivalSyncGroup.EVENT;
        }

        return switch (safe(item.getCategory())) {
            case "축제" -> FestivalSyncGroup.FESTIVAL;
            case "공연" -> FestivalSyncGroup.PERFORMANCE;
            default -> FestivalSyncGroup.EVENT;
        };
    }

    private FestivalSyncGroup resolveGroup(FestivalContent content) {
        if (content == null) {
            return FestivalSyncGroup.EVENT;
        }

        if ("12".equals(content.getContentTypeId())) {
            return FestivalSyncGroup.TOURIST_ATTRACTION;
        }

        if ("14".equals(content.getContentTypeId())) {
            return FestivalSyncGroup.CULTURAL_FACILITY;
        }

        if ("28".equals(content.getContentTypeId())) {
            return FestivalSyncGroup.LEPORTS;
        }

        return switch (safe(content.getCategory())) {
            case "축제" -> FestivalSyncGroup.FESTIVAL;
            case "공연" -> FestivalSyncGroup.PERFORMANCE;
            default -> FestivalSyncGroup.EVENT;
        };
    }

    private boolean hasDetailContent(FestivalContent content) {
        if (content == null) {
            return false;
        }

        return hasText(content.getOverview())
                || hasText(content.getHomepage())
                || hasText(content.getDescription())
                || safe(content.getDescriptionSource())
                .startsWith("DETAIL_COMMON")
                || hasMeaningfulJsonArray(content.getMainInfoJson())
                || content.getDetailSourceUpdatedAt() != null;
    }

    private boolean isSourceModifiedAfterDetail(
            FestivalContent content,
            LocalDateTime sourceUpdatedAt
    ) {
        if (content == null || sourceUpdatedAt == null) {
            return false;
        }

        LocalDateTime detailSourceUpdatedAt =
                content.getDetailSourceUpdatedAt();

        return detailSourceUpdatedAt != null
                && sourceUpdatedAt.isAfter(detailSourceUpdatedAt);
    }

    private void deactivateMissingContents(
            String contentTypeId,
            Set<String> currentContentIds,
            SyncCounter counter
    ) {
        deactivateMissingContents(
                contentTypeId,
                (Collection<String>) currentContentIds,
                counter
        );
    }

    private List<FestivalSyncGroupResultResponse> buildGroupResults(
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap
    ) {
        List<FestivalSyncGroupResultResponse> results = new ArrayList<>();

        for (FestivalSyncGroup group : FestivalSyncGroup.orderedGroups()) {
            GroupSyncWork work = groupWorkMap.get(group);

            results.add(
                    FestivalSyncGroupResultResponse.builder()
                            .group(group.getCode())
                            .groupName(group.getDisplayName())
                            .contentTypeId(group.getContentTypeId())
                            .category(group.getCategory())
                            .targetCount(work.targetCount())
                            .mainSyncedCount(work.mainSyncedCount())
                            .detailSyncedCount(work.detailSyncedCount())
                            .detailEmptyCount(work.detailEmptyCount())
                            .skippedCount(work.skippedCount())
                            .failedCount(work.failedCount())
                            .tourApiCallCount(work.apiCallCount())
                            .completed(
                                    work.failedCount() == 0
                                            && work.skippedCount() == 0
                            )
                            .build()
            );
        }

        return results;
    }

    private FestivalSyncResultResponse buildResponse(
            SyncCounter counter,
            int festivalPage,
            int festivalSize,
            int experiencePage,
            int experienceSize,
            int festivalStartPage,
            int festivalEndPage,
            int experienceStartPage,
            int experienceEndPage,
            int maxPages,
            int detailLimit,
            boolean includeDetail,
            boolean automaticSync,
            boolean deactivateMissing,
            boolean sequential,
            String eventStartDate,
            String message,
            List<FestivalSyncGroupResultResponse> groupResults
    ) {
        return FestivalSyncResultResponse.builder()
                .festivalSavedCount(counter.festivalSavedCount())
                .experienceSavedCount(counter.experienceSavedCount())
                .detailSyncedCount(counter.detailSyncedCount())
                .insertedCount(counter.insertedCount())
                .updatedCount(counter.updatedCount())
                .deactivatedCount(counter.deactivatedCount())
                .tourApiCallCount(counter.tourApiCallCount())
                .skippedDetailCount(counter.skippedDetailCount())
                .detailEmptyCount(counter.detailEmptyCount())
                .failedRequestCount(counter.failedRequestCount())
                .festivalPage(festivalPage)
                .festivalSize(festivalSize)
                .experiencePage(experiencePage)
                .experienceSize(experienceSize)
                .festivalStartPage(festivalStartPage)
                .festivalEndPage(festivalEndPage)
                .experienceStartPage(experienceStartPage)
                .experienceEndPage(experienceEndPage)
                .maxPages(maxPages)
                .detailLimit(detailLimit)
                .includeDetail(includeDetail)
                .automaticSync(automaticSync)
                .deactivateMissing(deactivateMissing)
                .sequential(sequential)
                .eventStartDate(eventStartDate)
                .message(message)
                .groupResults(groupResults == null ? List.of() : groupResults)
                .build();
    }

    private FestivalSyncResultResponse emptySyncResponse(String message) {
        return buildResponse(
                new SyncCounter(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                false,
                false,
                false,
                "",
                message,
                List.of()
        );
    }

    private String buildRefreshMessage(
            SyncRequest request,
            boolean allListsSafe,
            boolean deactivationApplied,
            boolean deactivationDeferred,
            SyncCounter counter
    ) {
        if (counter.failedRequestCount() > 0) {
            return "일부 TourAPI 또는 DB 저장 요청이 실패했습니다. 성공한 목록과 상세만 반영했고, 실패·이미지 미완료 대상은 다음 새벽에 재시도합니다.";
        }

        if (request.deactivateMissing() && deactivationDeferred) {
            return "목록은 저장했지만 페이지 수집 또는 원본 totalCount 검증이 완전하지 않아 비활성화 처리를 보류했습니다.";
        }

        if (counter.skippedDetailCount() > 0) {
            return "목록 갱신을 완료했고, 호출 예산 제한으로 남은 상세 대상은 다음 새벽에 그룹 순환 방식으로 이어서 처리합니다.";
        }

        if (counter.detailEmptyCount() > 0) {
            return "목록 갱신을 완료했습니다. 일부 콘텐츠는 원본 상세 공통 또는 유형별 정보가 비어 있어 제공 가능한 정보만 저장했습니다.";
        }

        if (request.deactivateMissing() && deactivationApplied && allListsSafe) {
            return "새벽 자동 refresh를 완료했습니다. 목록 갱신, 변경 상세 갱신, 안전한 비활성화 처리가 완료되었습니다.";
        }

        return request.automatic()
                ? "새벽 자동 refresh를 완료했습니다. 변경된 콘텐츠와 미완료 이미지만 보강했습니다."
                : "축제·체험 동기화를 완료했습니다.";
    }

    private String buildDetailOnlyMessage(SyncCounter counter) {
        if (counter.failedRequestCount() > 0) {
            return "상세 보강 중 일부 요청이 실패했습니다. 실패한 대상은 다음 실행에서 다시 처리합니다.";
        }

        if (counter.skippedDetailCount() > 0) {
            return "상세 보강은 호출 예산까지 처리했고 남은 대상은 다음 실행에서 이어서 처리합니다.";
        }

        return "상세 및 이미지 보강을 완료했습니다.";
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

            String key = row[0] == null
                    ? "UNKNOWN"
                    : safe(String.valueOf(row[0]));

            if (!hasText(key)) {
                key = "UNKNOWN";
            }

            long value = row[1] instanceof Number number
                    ? number.longValue()
                    : 0L;

            result.put(key, value);
        }

        return result;
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

    private LocalDateTime parseModifiedTime(String value) {
        if (!hasText(value) || !value.trim().matches("\\d{14}")) {
            return null;
        }

        try {
            return LocalDateTime.parse(
                    value.trim(),
                    TOUR_API_MODIFIED_TIME_FORMAT
            );
        } catch (Exception e) {
            return null;
        }
    }

    private boolean canCallApi(
            SyncCounter counter,
            int maxApiCalls,
            int requiredApiCalls
    ) {
        return counter.tourApiCallCount() + requiredApiCalls <= maxApiCalls;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }

        return Math.min(size, MAX_SIZE);
    }

    private int normalizeMaxPages(int maxPages) {
        if (maxPages < 1) {
            return DEFAULT_MAX_PAGES;
        }

        return Math.min(maxPages, MAX_MAX_PAGES);
    }

    private int normalizeDetailLimit(int detailLimit) {
        if (detailLimit < 1) {
            return DEFAULT_DETAIL_LIMIT;
        }

        return Math.min(detailLimit, MAX_DETAIL_LIMIT);
    }

    private int normalizeMaxApiCalls(int maxApiCalls) {
        if (maxApiCalls < 1) {
            return DEFAULT_MAX_API_CALLS;
        }

        return Math.min(maxApiCalls, MAX_MAX_API_CALLS);
    }

    private String normalizeEventStartDate(String eventStartDate) {
        if (hasText(eventStartDate)
                && eventStartDate.trim().matches("\\d{8}")) {
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

    private boolean hasMeaningfulJsonArray(String value) {
        return hasText(value) && !"[]".equals(value.trim());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record SyncRequest(
            int size,
            int maxPages,
            String eventStartDate,
            boolean includeDetail,
            int detailLimit,
            int maxApiCalls,
            boolean deactivateMissing,
            boolean forceAllDetails,
            boolean automatic
    ) {
    }

    private record SourceItem<T>(
            T item,
            LocalDateTime sourceUpdatedAt
    ) {
    }

    private record FetchResult<T>(
            List<SourceItem<T>> items,
            Set<String> contentIds,
            int lastPage,
            int sourceTotalCount,
            int apiCallCount,
            boolean completed
    ) {
        private boolean isSafeForDeactivation() {
            return completed
                    && sourceTotalCount >= 0
                    && contentIds.size() == sourceTotalCount;
        }
    }

    private record SummarySaveResult(
            boolean saved,
            DetailTask detailTask
    ) {
        private static SummarySaveResult empty() {
            return new SummarySaveResult(false, null);
        }
    }

    private record DetailTask(
            String contentId,
            String contentTypeId,
            LocalDateTime sourceUpdatedAt,
            DetailTaskType type
    ) {
    }

    private enum DetailTaskType {
        FULL(3),
        IMAGE_ONLY(1);

        private final int requiredApiCalls;

        DetailTaskType(int requiredApiCalls) {
            this.requiredApiCalls = requiredApiCalls;
        }

        public int requiredApiCalls() {
            return requiredApiCalls;
        }
    }

    private enum DetailSyncOutcome {
        SUCCESS,
        COMMON_ONLY,
        EMPTY,
        PARTIAL,
        FAILURE
    }

    private record ApiResponse(
            String rawJson,
            boolean success
    ) {
        private static ApiResponse success(String rawJson) {
            return new ApiResponse(rawJson, true);
        }

        private static ApiResponse failed() {
            return new ApiResponse("", false);
        }
    }

    private record CounterSnapshot(
            int tourApiCallCount
    ) {
        private static CounterSnapshot from(SyncCounter counter) {
            return new CounterSnapshot(counter.tourApiCallCount());
        }
    }

    private static class GroupSyncWork {

        private int targetCount;
        private int mainSyncedCount;
        private int detailSyncedCount;
        private int detailEmptyCount;
        private int skippedCount;
        private int failedCount;
        private int apiCallCount;

        private final List<DetailTask> detailTasks = new ArrayList<>();

        public int targetCount() {
            return targetCount;
        }

        public int mainSyncedCount() {
            return mainSyncedCount;
        }

        public int detailSyncedCount() {
            return detailSyncedCount;
        }

        public int detailEmptyCount() {
            return detailEmptyCount;
        }

        public int skippedCount() {
            return skippedCount;
        }

        public int failedCount() {
            return failedCount;
        }

        public int apiCallCount() {
            return apiCallCount;
        }

        public List<DetailTask> detailTasks() {
            return detailTasks;
        }

        public void increaseTargetCount() {
            targetCount++;
        }

        public void increaseMainSyncedCount() {
            mainSyncedCount++;
        }

        public void increaseDetailSyncedCount() {
            detailSyncedCount++;
        }

        public void increaseDetailEmptyCount() {
            detailEmptyCount++;
        }

        public void addSkippedCount(int count) {
            skippedCount += Math.max(count, 0);
        }

        public void increaseFailedCount() {
            failedCount++;
        }

        public void addApiCallCount(int count) {
            apiCallCount += Math.max(count, 0);
        }

        public void addDetailTask(DetailTask task) {
            if (task != null && hasValue(task.contentId())) {
                detailTasks.add(task);
            }
        }

        private static boolean hasValue(String value) {
            return value != null && !value.isBlank();
        }
    }

    private static class SyncCounter {

        private int festivalSavedCount;
        private int experienceSavedCount;
        private int detailSyncedCount;
        private int insertedCount;
        private int updatedCount;
        private int deactivatedCount;
        private int tourApiCallCount;
        private int skippedDetailCount;
        private int detailEmptyCount;
        private int failedRequestCount;

        public int festivalSavedCount() {
            return festivalSavedCount;
        }

        public int experienceSavedCount() {
            return experienceSavedCount;
        }

        public int detailSyncedCount() {
            return detailSyncedCount;
        }

        public int insertedCount() {
            return insertedCount;
        }

        public int updatedCount() {
            return updatedCount;
        }

        public int deactivatedCount() {
            return deactivatedCount;
        }

        public int tourApiCallCount() {
            return tourApiCallCount;
        }

        public int skippedDetailCount() {
            return skippedDetailCount;
        }

        public int detailEmptyCount() {
            return detailEmptyCount;
        }

        public int failedRequestCount() {
            return failedRequestCount;
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

        public void addDeactivatedCount(int count) {
            deactivatedCount += Math.max(count, 0);
        }

        public void increaseTourApiCallCount() {
            tourApiCallCount++;
        }

        public void addSkippedDetailCount(int count) {
            skippedDetailCount += Math.max(count, 0);
        }

        public void increaseDetailEmptyCount() {
            detailEmptyCount++;
        }

        public void increaseFailedRequestCount() {
            failedRequestCount++;
        }
    }
}
