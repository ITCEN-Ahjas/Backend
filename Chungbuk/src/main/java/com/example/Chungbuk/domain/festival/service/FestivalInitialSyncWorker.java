package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.global.exception.TourApiQuotaExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalInitialSyncWorker {

    private static final int DEFAULT_SIZE = 30;
    private static final int DEFAULT_MAX_PAGES = 50;
    private static final int DEFAULT_MAX_API_CALLS = 900;

    private final FestivalSyncService festivalSyncService;

    private final FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    private final FestivalInitialSyncRunGuard festivalInitialSyncRunGuard;

    @Async("festivalInitialSyncExecutor")
    public void runInitialSync(FestivalInitialSyncPhase phase) {
        try {
            FestivalSyncResultResponse result = executeSync(phase);

            if (result.isDailyQuotaExceeded()) {
                festivalInitialSyncStateService.waitForDailyQuota(
                        "당일 TourAPI 호출 한도에 도달해 "
                                + "다음 자동 실행에서 초기 적재를 이어서 처리합니다."
                );

                log.info(
                        "축제·체험 초기 적재가 일일 호출 한도에 도달해 대기 상태로 전환되었습니다."
                );

                return;
            }

            if (phase == FestivalInitialSyncPhase.LIST_SYNCING) {
                festivalInitialSyncStateService
                        .completeListPhaseAndReconcile();
            } else {
                festivalInitialSyncStateService
                        .completeCurrentPhaseAndReconcile();
            }

            log.info(
                    """
                    축제·체험 초기 적재 작업 완료
                    - phase={}
                    - festivalSavedCount={}
                    - experienceSavedCount={}
                    - detailSyncedCount={}
                    - failedRequestCount={}
                    - tourApiCallCount={}
                    """,
                    phase,
                    result.getFestivalSavedCount(),
                    result.getExperienceSavedCount(),
                    result.getDetailSyncedCount(),
                    result.getFailedRequestCount(),
                    result.getTourApiCallCount()
            );
        } catch (TourApiQuotaExceededException e) {
            festivalInitialSyncStateService.waitForDailyQuota(
                    "당일 TourAPI 호출 한도가 부족합니다: "
                            + safeMessage(e)
            );

            log.warn(
                    "축제·체험 초기 적재가 호출 한도 부족으로 중단되었습니다.",
                    e
            );
        } catch (RestClientException e) {
            festivalInitialSyncStateService.waitForTourApiError(
                    "TourAPI 요청 중 오류가 발생했습니다: "
                            + safeMessage(e)
            );

            log.warn(
                    "축제·체험 초기 적재가 TourAPI 요청 오류로 대기 상태가 되었습니다.",
                    e
            );
        } catch (Exception e) {
            festivalInitialSyncStateService.markFailed(
                    "초기 적재 실행 중 오류가 발생했습니다: "
                            + safeMessage(e)
            );

            log.error(
                    "축제·체험 초기 적재 실행 중 처리하지 못한 오류가 발생했습니다.",
                    e
            );
        } finally {
            festivalInitialSyncRunGuard.finish();
        }
    }

    private FestivalSyncResultResponse executeSync(
            FestivalInitialSyncPhase phase
    ) {
        if (phase == FestivalInitialSyncPhase.LIST_SYNCING) {
            return festivalSyncService.bootstrapFestivalContents(
                    DEFAULT_SIZE,
                    DEFAULT_MAX_PAGES,
                    DEFAULT_MAX_API_CALLS,
                    true
            );
        }

        return festivalSyncService.refreshFestivalContents(
                DEFAULT_SIZE,
                DEFAULT_MAX_PAGES,
                DEFAULT_MAX_API_CALLS,
                true
        );
    }

    private String safeMessage(Exception exception) {
        if (exception.getMessage() == null
                || exception.getMessage().isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return exception.getMessage();
    }
}