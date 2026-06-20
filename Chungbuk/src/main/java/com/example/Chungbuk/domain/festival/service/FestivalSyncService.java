package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.client.TourApiClient;
import com.example.Chungbuk.domain.festival.constant.FestivalDetailRetryReason;
import com.example.Chungbuk.domain.festival.constant.FestivalSyncGroup;
import com.example.Chungbuk.domain.festival.constant.SupportedContentType;
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
import com.example.Chungbuk.global.exception.TourApiQuotaExceededException;
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
import java.util.function.Supplier;
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

    /*
     * 실행 1회당 최대 호출 수다.
     * 실제 하루 전체 호출량은 TourApiQuotaService에서 DB 기준으로 관리한다.
     */
    private static final int DEFAULT_MAX_API_CALLS = 900;
    private static final int MAX_MAX_API_CALLS = 900;

    private static final int DEFAULT_DETAIL_LIMIT = 1000;
    private static final int MAX_DETAIL_LIMIT = 2000;

    private static final String DEFAULT_FESTIVAL_CONTENT_TYPE_ID = "15";
    private static final String DEFAULT_EVENT_START_DATE = "20230101";

    private static final List<String> SUPPORTED_CONTENT_TYPE_IDS =
            List.of("12", "14", "15", "28");

    private static final DateTimeFormatter TOUR_API_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final DateTimeFormatter TOUR_API_MODIFIED_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TourApiClient tourApiClient;
    private final FestivalMapper festivalMapper;
    private final FestivalContentRepository festivalContentRepository;
    private final PlatformTransactionManager transactionManager;
    private final TourApiQuotaService tourApiQuotaService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /*
     * 현재 단일 서버 인스턴스에서 Scheduler와 수동 sync가
     * 동시에 실행되지 않도록 막는다.
     */
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

    /*
     * 새벽 Scheduler 전용 refresh.
     * 신규·수정·상세 미완료·이미지 미완료 대상만 보강한다.
     */
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
     * 최초 전체 적재.
     * 기존 목록 누락 데이터를 비활성화하지 않고, 상세 전체 보강을 시도한다.
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
     * 초기 적재 1단계: 목록만 저장한다.
     * 목록 수집이 완전하게 끝나기 전에는 기존 콘텐츠를 비활성화하지 않는다.
     */
    public FestivalSyncResultResponse syncInitialLists(
            int size,
            int maxPages,
            int maxApiCalls
    ) {
        return runListRefresh(
                new SyncRequest(
                        normalizeSize(size),
                        normalizeMaxPages(maxPages),
                        DEFAULT_EVENT_START_DATE,
                        false,
                        0,
                        normalizeMaxApiCalls(maxApiCalls),
                        false,
                        false,
                        false
                )
        );
    }

    /*
     * 수동 refresh.
     * Scheduler와 같은 변경 감지·안전 비활성화 흐름을 사용한다.
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

    /*
     * 과거 호환용 메서드.
     * 다음 커밋에서 Controller·Swagger 정리 시 제거 대상이다.
     */
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
        return runStoredDetailSync(
                normalizeDetailLimit(limit),
                normalizeMaxApiCalls(maxApiCalls),
                onlyMissing
                        ? StoredDetailTargetMode.PENDING
                        : StoredDetailTargetMode.ALL
        );
    }

    /*
     * 초기 적재 2단계: 상세 소개·홈페이지·유형별 주요 정보만 저장한다.
     * 상세 이미지 확인은 IMAGE_SYNCING 단계에서 별도로 처리한다.
     */
    public FestivalSyncResultResponse syncInitialDetails(
            int limit,
            int maxApiCalls
    ) {
        return runStoredDetailSync(
                normalizeDetailLimit(limit),
                normalizeMaxApiCalls(maxApiCalls),
                StoredDetailTargetMode.INITIAL_DETAIL_ONLY
        );
    }

    /*
     * 초기 적재 3단계: 상세 완료 콘텐츠 중 이미지 확인이 끝나지 않은 대상만 처리한다.
     */
    public FestivalSyncResultResponse syncInitialImages(
            int limit,
            int maxApiCalls
    ) {
        return runStoredDetailSync(
                normalizeDetailLimit(limit),
                normalizeMaxApiCalls(maxApiCalls),
                StoredDetailTargetMode.INITIAL_IMAGE_ONLY
        );
    }

    private FestivalSyncResultResponse runStoredDetailSync(
            int limit,
            int maxApiCalls,
            StoredDetailTargetMode targetMode
    ) {
        if (!syncRunning.compareAndSet(false, true)) {
            return emptySyncResponse(
                    "축제·체험 동기화가 이미 실행 중입니다. "
                            + "기존 요청이 끝난 뒤 다시 실행하세요."
            );
        }

        try {
            LocalDateTime syncedAt = LocalDateTime.now();
            SyncCounter counter = new SyncCounter();

            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                    createGroupWorkMap();

            addStoredDetailTargets(
                    groupWorkMap,
                    targetMode,
                    limit,
                    syncedAt
            );

            processDetailTasksRoundRobin(
                    groupWorkMap,
                    maxApiCalls,
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
                    limit,
                    true,
                    false,
                    false,
                    true,
                    false,
                    "",
                    buildDetailOnlyMessage(counter),
                    buildGroupResults(groupWorkMap)
            );
        } finally {
            syncRunning.set(false);
        }
    }

    public FestivalSyncResultResponse syncFestivalContentDetail(
            String contentId,
            boolean includeImages
    ) {
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
                    "축제·체험 동기화가 이미 실행 중입니다. "
                            + "기존 요청이 끝난 뒤 다시 실행하세요."
            );
        }

        try {
            FestivalContent content = festivalContentRepository
                    .findByContentId(contentId)
                    .orElse(null);

            if (content == null) {
                return emptySyncResponse(
                        "DB에 저장된 콘텐츠가 없습니다. "
                                + "목록 동기화 후 다시 실행하세요."
                );
            }

            LocalDateTime syncedAt = LocalDateTime.now();
            SyncCounter counter = new SyncCounter();

            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap =
                    createGroupWorkMap();

            FestivalSyncGroup group = resolveGroup(content);

            groupWorkMap.get(group).addDetailTask(
                    new DetailTask(
                            content.getContentId(),
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
                    false,
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
                    "축제·체험 동기화가 이미 실행 중입니다. "
                            + "기존 요청이 끝난 뒤 다시 실행하세요."
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

            groupWorkMap.get(FestivalSyncGroup.FESTIVAL)
                    .addApiCallCount(eventFetchResult.apiCallCount());

            for (SourceItem<FestivalSummaryResponse> sourceItem
                    : eventFetchResult.items()) {
                FestivalSyncGroup group = resolveFestivalGroup(
                        sourceItem.item()
                );

                GroupSyncWork groupWork = groupWorkMap.get(group);

                groupWork.increaseTargetCount();

                SummarySaveResult saveResult = saveFestivalSummary(
                        sourceItem,
                        syncedAt,
                        request.includeDetail(),
                        request.forceAllDetails()
                );

                increaseSummaryCounter(
                        saveResult,
                        counter,
                        true
                );

                groupWork.addDetailTask(saveResult.detailTask());

                if (saveResult.saved()) {
                    groupWork.increaseMainSyncedCount();
                }
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

                GroupSyncWork groupWork = groupWorkMap.get(group);

                groupWork.addApiCallCount(fetchResult.apiCallCount());

                for (SourceItem<ExperienceSummaryResponse> sourceItem
                        : fetchResult.items()) {
                    groupWork.increaseTargetCount();

                    SummarySaveResult saveResult =
                            saveExperienceSummary(
                                    sourceItem,
                                    syncedAt,
                                    request.includeDetail(),
                                    request.forceAllDetails()
                            );

                    increaseSummaryCounter(
                            saveResult,
                            counter,
                            false
                    );

                    groupWork.addDetailTask(saveResult.detailTask());

                    if (saveResult.saved()) {
                        groupWork.increaseMainSyncedCount();
                    }
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
                    allListsSafe,
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
        List<SourceItem<FestivalSummaryResponse>> items =
                new ArrayList<>();

        Set<String> contentIds = new LinkedHashSet<>();

        int totalPages = 1;
        int lastPage = 0;
        int sourceTotalCount = -1;
        int apiCallCount = 0;
        boolean completed = true;

        for (int page = 1; page <= totalPages; page++) {
            if (page > maxPages || !canCallApi(
                    counter,
                    maxApiCalls,
                    1
            )) {
                completed = false;
                break;
            }

            int beforeCallCount = counter.tourApiCallCount();

            ApiResponse response = requestFestivalListRaw(
                    page,
                    size,
                    eventStartDate,
                    counter
            );

            apiCallCount += counter.tourApiCallCount()
                    - beforeCallCount;

            if (!response.success()) {
                completed = false;
                break;
            }

            int pageTotalCount = readTourApiTotalCount(
                    response.rawJson()
            );

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

                items.add(new SourceItem<>(
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
        List<SourceItem<ExperienceSummaryResponse>> items =
                new ArrayList<>();

        Set<String> contentIds = new LinkedHashSet<>();

        int totalPages = 1;
        int lastPage = 0;
        int sourceTotalCount = -1;
        int apiCallCount = 0;
        boolean completed = true;

        for (int page = 1; page <= totalPages; page++) {
            if (page > maxPages || !canCallApi(
                    counter,
                    maxApiCalls,
                    1
            )) {
                completed = false;
                break;
            }

            int beforeCallCount = counter.tourApiCallCount();

            ApiResponse response = requestExperienceListRaw(
                    page,
                    size,
                    contentTypeId,
                    counter
            );

            apiCallCount += counter.tourApiCallCount()
                    - beforeCallCount;

            if (!response.success()) {
                completed = false;
                break;
            }

            int pageTotalCount = readTourApiTotalCount(
                    response.rawJson()
            );

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

                items.add(new SourceItem<>(
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
            boolean forceAllDetails
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

        FestivalContent savedContent = festivalContentRepository.save(content);

        return new SummarySaveResult(
                true,
                inserted,
                createDetailTask(
                        savedContent,
                        includeDetail,
                        forceAllDetails,
                        sourceItem.sourceUpdatedAt(),
                        syncedAt
                )
        );
    }

    private SummarySaveResult saveExperienceSummary(
            SourceItem<ExperienceSummaryResponse> sourceItem,
            LocalDateTime syncedAt,
            boolean includeDetail,
            boolean forceAllDetails
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

        FestivalContent savedContent = festivalContentRepository.save(content);

        return new SummarySaveResult(
                true,
                inserted,
                createDetailTask(
                        savedContent,
                        includeDetail,
                        forceAllDetails,
                        sourceItem.sourceUpdatedAt(),
                        syncedAt
                )
        );
    }

    private void increaseSummaryCounter(
            SummarySaveResult saveResult,
            SyncCounter counter,
            boolean festival
    ) {
        if (!saveResult.saved()) {
            return;
        }

        if (festival) {
            counter.increaseFestivalSavedCount();
        } else {
            counter.increaseExperienceSavedCount();
        }

        if (saveResult.inserted()) {
            counter.increaseInsertedCount();
        } else {
            counter.increaseUpdatedCount();
        }
    }

    private DetailTask createDetailTask(
            FestivalContent content,
            boolean includeDetail,
            boolean forceAllDetails,
            LocalDateTime sourceUpdatedAt,
            LocalDateTime now
    ) {
        if (!includeDetail
                || content == null
                || !hasText(content.getContentId())) {
            return null;
        }

        if (forceAllDetails) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    DetailTaskType.FULL
            );
        }

        /*
         * 원본 수정 시각이 더 최신이면 재시도 대기 상태보다 우선한다.
         */
        if (isSourceModifiedAfterDetail(content, sourceUpdatedAt)) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    DetailTaskType.FULL
            );
        }

        if (content.isDetailRetryScheduled(now)) {
            return null;
        }

        if (content.isDetailRetryDue(now)) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    sourceUpdatedAt,
                    content.isImageOnlyRetry()
                            ? DetailTaskType.IMAGE_ONLY
                            : DetailTaskType.FULL
            );
        }

        if (!hasDetailContent(content)) {
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
            StoredDetailTargetMode targetMode,
            int limit,
            LocalDateTime now
    ) {
        List<FestivalContent> contents = festivalContentRepository
                .findAllByActiveTrueAndContentTypeIdIn(
                        SUPPORTED_CONTENT_TYPE_IDS
                );

        int addedCount = 0;

        for (FestivalContent content : contents) {
            if (addedCount >= limit) {
                break;
            }

            DetailTask task = createStoredDetailTask(
                    content,
                    targetMode,
                    now
            );

            if (task == null) {
                continue;
            }

            groupWorkMap.get(resolveGroup(content))
                    .addDetailTask(task);

            addedCount++;
        }
    }

    private DetailTask createStoredDetailTask(
            FestivalContent content,
            StoredDetailTargetMode targetMode,
            LocalDateTime now
    ) {
        if (content == null || !hasText(content.getContentId())) {
            return null;
        }

        if (targetMode == StoredDetailTargetMode.ALL) {
            return new DetailTask(
                    content.getContentId(),
                    content.getContentTypeId(),
                    content.getSourceUpdatedAt(),
                    DetailTaskType.FULL
            );
        }

        DetailTask pendingTask = createDetailTask(
                content,
                true,
                false,
                content.getSourceUpdatedAt(),
                now
        );

        if (pendingTask == null) {
            return null;
        }

        if (targetMode == StoredDetailTargetMode.PENDING) {
            return pendingTask;
        }

        if (targetMode == StoredDetailTargetMode.INITIAL_DETAIL_ONLY
                && pendingTask.type() == DetailTaskType.FULL) {
            return new DetailTask(
                    pendingTask.contentId(),
                    pendingTask.contentTypeId(),
                    pendingTask.sourceUpdatedAt(),
                    DetailTaskType.DETAIL_ONLY
            );
        }

        if (targetMode == StoredDetailTargetMode.INITIAL_IMAGE_ONLY
                && pendingTask.type() == DetailTaskType.IMAGE_ONLY) {
            return pendingTask;
        }

        return null;
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

                if (!canCallApi(
                        counter,
                        maxApiCalls,
                        task.type().requiredApiCalls()
                )) {
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
                        after.tourApiCallCount()
                                - before.tourApiCallCount()
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
            recordDetailRetry(
                    task.contentId(),
                    FestivalDetailRetryReason.DETAIL_SAVE_FAILED,
                    syncedAt
            );

            counter.increaseFailedRequestCount();

            return DetailSyncOutcome.FAILURE;
        } catch (RuntimeException e) {
            recordDetailRetry(
                    task.contentId(),
                    FestivalDetailRetryReason.DETAIL_SAVE_FAILED,
                    syncedAt
            );

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
            if (!commonResponse.quotaExceeded()) {
                recordDetailRetry(
                        task.contentId(),
                        FestivalDetailRetryReason
                                .DETAIL_COMMON_REQUEST_FAILED,
                        syncedAt
                );
            }

            return DetailSyncOutcome.FAILURE;
        }

        if (!festivalMapper.hasDetailItem(commonResponse.rawJson())) {
            recordDetailRetry(
                    task.contentId(),
                    FestivalDetailRetryReason.DETAIL_COMMON_EMPTY,
                    syncedAt
            );

            return DetailSyncOutcome.EMPTY;
        }

        FestivalContent existingContent = festivalContentRepository
                .findByContentId(task.contentId())
                .orElse(null);

        String expectedContentTypeId = firstNonBlank(
                task.contentTypeId(),
                existingContent == null
                        ? ""
                        : existingContent.getContentTypeId(),
                festivalMapper.extractContentTypeId(
                        commonResponse.rawJson()
                )
        );

        if (!SupportedContentType.isSupported(expectedContentTypeId)) {
            recordDetailRetry(
                    task.contentId(),
                    FestivalDetailRetryReason.DETAIL_RESPONSE_INVALID,
                    syncedAt
            );

            counter.increaseFailedRequestCount();

            return DetailSyncOutcome.FAILURE;
        }

        ApiResponse introResponse = requestDetailIntroRaw(
                task.contentId(),
                expectedContentTypeId,
                counter
        );

        if (!introResponse.success()) {
            if (!introResponse.quotaExceeded()) {
                recordDetailRetry(
                        task.contentId(),
                        FestivalDetailRetryReason
                                .DETAIL_INTRO_REQUEST_FAILED,
                        syncedAt
                );
            }

            return DetailSyncOutcome.FAILURE;
        }

        boolean imageRequested = task.type() != DetailTaskType.DETAIL_ONLY;

        ApiResponse imageResponse = imageRequested
                ? requestDetailImageRaw(task.contentId(), counter)
                : ApiResponse.success("");

        boolean imageSynced = imageRequested && imageResponse.success();

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

        if (!isPersistableDetailResponse(
                detailResponse,
                task,
                expectedContentTypeId
        )) {
            recordDetailRetry(
                    task.contentId(),
                    FestivalDetailRetryReason.DETAIL_RESPONSE_INVALID,
                    syncedAt
            );

            counter.increaseFailedRequestCount();

            return DetailSyncOutcome.FAILURE;
        }

        saveDetail(
                detailResponse,
                task.sourceUpdatedAt(),
                imageRequested,
                imageSynced,
                syncedAt,
                counter
        );

        if (imageRequested && !imageSynced) {
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
            if (!imageResponse.quotaExceeded()) {
                recordDetailRetry(
                        task.contentId(),
                        FestivalDetailRetryReason
                                .DETAIL_IMAGE_REQUEST_FAILED,
                        syncedAt
                );
            }

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

    /*
     * Mapper가 빈 DTO나 기본 타입 15 DTO를 만들더라도,
     * 저장 직전 검증에서 차단한다.
     */
    private boolean isPersistableDetailResponse(
            FestivalDetailResponse detailResponse,
            DetailTask task,
            String expectedContentTypeId
    ) {
        if (detailResponse == null
                || !hasText(detailResponse.getId())
                || !hasText(detailResponse.getContentTypeId())
                || !hasText(detailResponse.getTitle())) {
            return false;
        }

        if (!task.contentId().equals(detailResponse.getId())) {
            return false;
        }

        if (!SupportedContentType.isSupported(
                detailResponse.getContentTypeId()
        )) {
            return false;
        }

        return !hasText(expectedContentTypeId)
                || expectedContentTypeId.equals(
                detailResponse.getContentTypeId()
        );
    }

    private void saveDetail(
            FestivalDetailResponse detail,
            LocalDateTime sourceUpdatedAt,
            boolean imageRequested,
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

            if (imageRequested && imageSynced) {
                content.clearDetailRetry();
            } else if (imageRequested) {
                content.recordDetailRetry(
                        FestivalDetailRetryReason
                                .DETAIL_IMAGE_REQUEST_FAILED,
                        syncedAt
                );
            }

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

            content.updateImageInfo(
                    toJson(extractImageUrls(
                            imageRawJson,
                            content.getImageUrl()
                    )),
                    syncedAt
            );

            festivalContentRepository.save(content);

            counter.increaseUpdatedCount();
            counter.increaseDetailSyncedCount();
        });
    }

    private void recordDetailRetry(
            String contentId,
            FestivalDetailRetryReason reason,
            LocalDateTime retryRequestedAt
    ) {
        TransactionTemplate transactionTemplate =
                createRequiredNewTransactionTemplate();

        try {
            transactionTemplate.executeWithoutResult(status -> {
                FestivalContent content = festivalContentRepository
                        .findByContentId(contentId)
                        .orElse(null);

                if (content == null) {
                    return;
                }

                content.recordDetailRetry(reason, retryRequestedAt);

                festivalContentRepository.save(content);
            });
        } catch (DataAccessException ignored) {
            /*
             * 재시도 상태 저장까지 실패해도 기존 상세 데이터는 그대로 둔다.
             */
        }
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
        return executeTourApiCall(
                () -> tourApiClient.getFestivalListRaw(
                        page,
                        size,
                        eventStartDate,
                        null
                ),
                counter
        );
    }

    private ApiResponse requestExperienceListRaw(
            int page,
            int size,
            String contentTypeId,
            SyncCounter counter
    ) {
        return executeTourApiCall(
                () -> tourApiClient.getExperienceListRaw(
                        page,
                        size,
                        null,
                        contentTypeId
                ),
                counter
        );
    }

    private ApiResponse requestDetailCommonRaw(
            String contentId,
            SyncCounter counter
    ) {
        return executeTourApiCall(
                () -> tourApiClient.getFestivalDetailCommonRaw(
                        contentId
                ),
                counter
        );
    }

    private ApiResponse requestDetailIntroRaw(
            String contentId,
            String contentTypeId,
            SyncCounter counter
    ) {
        return executeTourApiCall(
                () -> tourApiClient.getFestivalDetailIntroRaw(
                        contentId,
                        contentTypeId
                ),
                counter
        );
    }

    private ApiResponse requestDetailImageRaw(
            String contentId,
            SyncCounter counter
    ) {
        return executeTourApiCall(
                () -> tourApiClient.getFestivalDetailImageRaw(
                        contentId
                ),
                counter
        );
    }

    private ApiResponse executeTourApiCall(
            Supplier<String> apiCall,
            SyncCounter counter
    ) {
        if (!hasAvailableDailyQuota(counter, 1)) {
            return ApiResponse.quotaExceededResponse();
        }

        try {
            String rawJson = apiCall.get();

            counter.increaseTourApiCallCount();

            return validateApiResponse(rawJson, counter);
        } catch (TourApiQuotaExceededException e) {
            counter.markDailyQuotaExceeded();

            return ApiResponse.quotaExceededResponse();
        } catch (RestClientException e) {
            /*
             * API 요청 시도는 발생했으므로 실행 기준 호출 수에도 포함한다.
             */
            counter.increaseTourApiCallCount();
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
                String contentId = item.path("contentid")
                        .asText("")
                        .trim();

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

    private List<String> extractImageUrls(
            String rawJson,
            String fallbackImageUrl
    ) {
        Set<String> imageUrlSet = new LinkedHashSet<>();

        if (hasText(fallbackImageUrl)) {
            imageUrlSet.add(fallbackImageUrl.trim());
        }

        try {
            JsonNode body = getBody(rawJson);

            JsonNode itemNode = body.path("items").path("item");

            for (JsonNode item : toItemList(itemNode)) {
                String originalImageUrl = item.path("originimgurl")
                        .asText("")
                        .trim();

                String smallImageUrl = item.path("smallimageurl")
                        .asText("")
                        .trim();

                if (hasText(originalImageUrl)) {
                    imageUrlSet.add(originalImageUrl);
                }

                if (hasText(smallImageUrl)) {
                    imageUrlSet.add(smallImageUrl);
                }
            }
        } catch (Exception ignored) {
            return new ArrayList<>(imageUrlSet);
        }

        return new ArrayList<>(imageUrlSet);
    }

    private JsonNode getBody(String rawJson)
            throws JsonProcessingException {
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
            JsonNode totalCountNode = getBody(rawJson)
                    .path("totalCount");

            if (totalCountNode.isMissingNode()
                    || totalCountNode.isNull()) {
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

        return detailSourceUpdatedAt == null
                || sourceUpdatedAt.isAfter(detailSourceUpdatedAt);
    }

    private List<FestivalSyncGroupResultResponse> buildGroupResults(
            Map<FestivalSyncGroup, GroupSyncWork> groupWorkMap
    ) {
        List<FestivalSyncGroupResultResponse> results =
                new ArrayList<>();

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
            boolean listSyncCompleted,
            String eventStartDate,
            String message,
            List<FestivalSyncGroupResultResponse> groupResults
    ) {
        TourApiQuotaService.QuotaSnapshot quotaSnapshot =
                tourApiQuotaService.getTodayQuotaSnapshot();

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
                .listSyncCompleted(listSyncCompleted)
                .eventStartDate(eventStartDate)
                .message(message)
                .quotaUsageDate(quotaSnapshot.usageDate())
                .dailyTourApiLimit(quotaSnapshot.dailyCallLimit())
                .dailyTourApiUsedCount(quotaSnapshot.usedCallCount())
                .dailyTourApiRemainingCount(
                        quotaSnapshot.remainingCallCount()
                )
                .dailyQuotaExceeded(counter.dailyQuotaExceeded())
                .groupResults(
                        groupResults == null
                                ? List.of()
                                : groupResults
                )
                .build();
    }

    private FestivalSyncResultResponse emptySyncResponse(
            String message
    ) {
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
        if (counter.dailyQuotaExceeded()) {
            return "오늘 TourAPI 일일 호출 예산이 부족해 남은 목록 또는 상세 작업을 "
                    + "보류했습니다. 다음 날 자동 refresh에서 이어서 처리합니다.";
        }

        if (counter.failedRequestCount() > 0) {
            return "일부 TourAPI 또는 DB 저장 요청이 실패했습니다. "
                    + "기존 데이터는 유지했고 실패 대상은 재시도 예정 시각 이후에 "
                    + "다시 처리합니다.";
        }

        if (request.deactivateMissing() && deactivationDeferred) {
            return "목록은 저장했지만 페이지 수집 또는 원본 totalCount 검증이 "
                    + "완전하지 않아 비활성화 처리를 보류했습니다.";
        }

        if (counter.skippedDetailCount() > 0) {
            return "목록 갱신을 완료했고, 실행 호출 예산 제한으로 남은 상세 대상은 "
                    + "다음 실행에서 이어서 처리합니다.";
        }

        if (counter.detailEmptyCount() > 0) {
            return "목록 갱신을 완료했습니다. 일부 콘텐츠의 원본 상세 정보가 "
                    + "비어 있어 재시도 예정 시각 이후에 다시 확인합니다.";
        }

        if (request.deactivateMissing()
                && deactivationApplied
                && allListsSafe) {
            return "새벽 자동 refresh를 완료했습니다. 목록 갱신, 변경 상세 갱신, "
                    + "안전한 비활성화 처리가 완료되었습니다.";
        }

        return request.automatic()
                ? "새벽 자동 refresh를 완료했습니다. 변경된 콘텐츠와 "
                + "미완료 이미지만 보강했습니다."
                : "축제·체험 동기화를 완료했습니다.";
    }

    private String buildDetailOnlyMessage(SyncCounter counter) {
        if (counter.dailyQuotaExceeded()) {
            return "오늘 TourAPI 일일 호출 예산이 부족해 남은 상세 작업을 "
                    + "보류했습니다. 다음 실행에서 이어서 처리합니다.";
        }

        if (counter.failedRequestCount() > 0) {
            return "상세 보강 중 일부 요청이 실패했습니다. 기존 데이터는 유지했고 "
                    + "실패 대상은 재시도 예정 시각 이후에 다시 처리합니다.";
        }

        if (counter.skippedDetailCount() > 0) {
            return "상세 보강은 실행 호출 예산까지 처리했고 남은 대상은 "
                    + "다음 실행에서 이어서 처리합니다.";
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
        if (!hasText(value)
                || !value.trim().matches("\\d{14}")) {
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
        if (counter.tourApiCallCount() + requiredApiCalls
                > maxApiCalls) {
            return false;
        }

        if (!tourApiQuotaService.hasAvailableCalls(requiredApiCalls)) {
            counter.markDailyQuotaExceeded();
            return false;
        }

        return true;
    }

    private boolean hasAvailableDailyQuota(
            SyncCounter counter,
            int requiredApiCalls
    ) {
        if (tourApiQuotaService.hasAvailableCalls(requiredApiCalls)) {
            return true;
        }

        counter.markDailyQuotaExceeded();
        return false;
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
            boolean inserted,
            DetailTask detailTask
    ) {
        private static SummarySaveResult empty() {
            return new SummarySaveResult(false, false, null);
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
        DETAIL_ONLY(2),
        IMAGE_ONLY(1);

        private final int requiredApiCalls;

        DetailTaskType(int requiredApiCalls) {
            this.requiredApiCalls = requiredApiCalls;
        }

        public int requiredApiCalls() {
            return requiredApiCalls;
        }
    }

    private enum StoredDetailTargetMode {
        ALL,
        PENDING,
        INITIAL_DETAIL_ONLY,
        INITIAL_IMAGE_ONLY
    }

    private enum DetailSyncOutcome {
        SUCCESS,
        COMMON_ONLY,
        EMPTY,
        PARTIAL,
        FAILURE
    }

    /*
     * quotaExceeded는 record의 boolean 접근자다.
     * quotaExceededResponse()는 일일 예산 소진 응답을 만드는 static 메서드다.
     * 이름 충돌이 나지 않도록 분리했다.
     */
    private record ApiResponse(
            String rawJson,
            boolean success,
            boolean quotaExceeded
    ) {
        private static ApiResponse success(String rawJson) {
            return new ApiResponse(rawJson, true, false);
        }

        private static ApiResponse failed() {
            return new ApiResponse("", false, false);
        }

        private static ApiResponse quotaExceededResponse() {
            return new ApiResponse("", false, true);
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
            if (task != null
                    && task.contentId() != null
                    && !task.contentId().isBlank()) {
                detailTasks.add(task);
            }
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
        private boolean dailyQuotaExceeded;

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

        public boolean dailyQuotaExceeded() {
            return dailyQuotaExceeded;
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

        public void markDailyQuotaExceeded() {
            dailyQuotaExceeded = true;
        }
    }
}