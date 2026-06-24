package com.example.Chungbuk.domain.accommodation.service;

import com.example.Chungbuk.domain.accommodation.client.AccommodationApiClient;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPauseReason;
import com.example.Chungbuk.domain.accommodation.constant.AccommodationSyncPhase;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationListResponse;
import com.example.Chungbuk.domain.accommodation.dto.response.AccommodationSummaryResponse;
import com.example.Chungbuk.domain.accommodation.entity.AccommodationEntity;
import com.example.Chungbuk.domain.accommodation.mapper.AccommodationMapper;
import com.example.Chungbuk.domain.accommodation.repository.AccommodationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccommodationInitialSyncWorker {

    private static final int LIST_PAGE_SIZE = 100;
    private static final int MAX_LIST_PAGES = 30;
    private static final int MAX_DETAIL_API_CALLS = 800;
    private static final int DETAIL_BATCH_SIZE = 50;
    private static final int DETAIL_API_CALLS_PER_ITEM = 4;

    private final AccommodationApiClient accommodationApiClient;
    private final AccommodationMapper accommodationMapper;
    private final AccommodationRepository accommodationRepository;
    private final AccommodationSyncStateService accommodationSyncStateService;
    private final AccommodationInitialSyncRunGuard accommodationInitialSyncRunGuard;
    private final AccommodationService accommodationService;

    @Async("accommodationInitialSyncExecutor")
    public void runInitialSync(AccommodationSyncPhase startPhase) {
        try {
            AccommodationSyncPhase currentPhase = startPhase;

            if (currentPhase == AccommodationSyncPhase.NOT_STARTED
                    || currentPhase == AccommodationSyncPhase.LIST_SYNCING) {
                long savedCount = runListSync();
                accommodationSyncStateService.completeListPhase(savedCount);
                currentPhase = AccommodationSyncPhase.DETAIL_SYNCING;

                log.info("숙소 목록 수집 완료. savedCount={}", savedCount);
            }

            if (currentPhase == AccommodationSyncPhase.DETAIL_SYNCING) {
                long syncedCount = runDetailSync();
                accommodationSyncStateService.completeAllSync(syncedCount);

                log.info("숙소 상세 수집 완료. syncedCount={}", syncedCount);
            }

        } catch (RestClientException e) {
            accommodationSyncStateService.markWaiting(
                    AccommodationSyncPauseReason.API_ERROR,
                    "TourAPI 요청 중 오류: " + safeMessage(e)
            );
            log.warn("숙소 배치 수집이 API 오류로 중단되었습니다.", e);

        } catch (Exception e) {
            accommodationSyncStateService.markFailed("배치 수집 중 오류: " + safeMessage(e));
            log.error("숙소 배치 수집 중 처리되지 않은 오류가 발생했습니다.", e);

        } finally {
            accommodationInitialSyncRunGuard.finish();
        }
    }

    private long runListSync() {
        long totalSaved = 0;

        for (int page = 1; page <= MAX_LIST_PAGES; page++) {
            String rawJson = accommodationApiClient.getAccommodationListRaw(
                    page, LIST_PAGE_SIZE, null
            );

            AccommodationListResponse response = accommodationMapper.toAccommodationListResponse(
                    rawJson, page, LIST_PAGE_SIZE
            );

            List<AccommodationSummaryResponse> items = response.getItems();

            if (items.isEmpty()) {
                break;
            }

            for (AccommodationSummaryResponse summary : items) {
                if (hasText(summary.getId())
                        && !accommodationRepository.existsById(summary.getId())) {
                    accommodationRepository.save(buildSummaryEntity(summary));
                    totalSaved++;
                }
            }

            log.info("숙소 목록 수집 중. page={}, items={}, totalSaved={}", page, items.size(), totalSaved);

            if (items.size() < LIST_PAGE_SIZE) {
                break;
            }
        }

        return totalSaved;
    }

    private long runDetailSync() {
        long totalSynced = 0;
        int apiCallCount = 0;

        while (apiCallCount < MAX_DETAIL_API_CALLS) {
            List<AccommodationEntity> pending = accommodationRepository
                    .findAllByDetailSyncedFalse(PageRequest.of(0, DETAIL_BATCH_SIZE));

            if (pending.isEmpty()) {
                break;
            }

            for (AccommodationEntity entity : pending) {
                if (apiCallCount >= MAX_DETAIL_API_CALLS) {
                    log.info("숙소 상세 수집 API 호출 한도 도달. apiCallCount={}", apiCallCount);
                    break;
                }

                try {
                    accommodationService.syncAccommodationDetail(entity.getId());
                    totalSynced++;
                } catch (Exception e) {
                    log.warn("숙소 상세 수집 실패. contentId={}", entity.getId(), e);
                } finally {
                    apiCallCount += DETAIL_API_CALLS_PER_ITEM;
                }
            }
        }

        return totalSynced;
    }

    private AccommodationEntity buildSummaryEntity(AccommodationSummaryResponse summary) {
        return AccommodationEntity.builder()
                .id(summary.getId())
                .cat1(summary.getCat1())
                .cat2(summary.getCat2())
                .cat3(summary.getCat3())
                .title(summary.getTitle())
                .region(summary.getRegion())
                .category(summary.getCategory())
                .address(summary.getAddress())
                .imageUrl(summary.getImageUrl())
                .tel(summary.getTel())
                .mapX(summary.getMapX())
                .mapY(summary.getMapY())
                .detailSynced(false)
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }
        return e.getMessage();
    }
}
