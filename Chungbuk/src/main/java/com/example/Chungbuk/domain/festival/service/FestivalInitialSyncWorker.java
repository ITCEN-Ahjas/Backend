package com.example.Chungbuk.domain.festival.service;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
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
    private static final int DEFAULT_DETAIL_LIMIT = 1000;

    private final FestivalSyncService festivalSyncService;

    private final FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    private final FestivalInitialSyncRunGuard festivalInitialSyncRunGuard;

    @Async("festivalInitialSyncExecutor")
    public void runInitialSync(FestivalInitialSyncPhase phase) {
        FestivalInitialSyncPhase currentPhase = phase;

        try {
            while (currentPhase != FestivalInitialSyncPhase.READY) {
                FestivalSyncResultResponse result = executeSync(
                        currentPhase
                );

                if (currentPhase == FestivalInitialSyncPhase.LIST_SYNCING
                        && !result.isListSyncCompleted()) {
                    handleIncompleteListResult(result);
                    return;
                }

                FestivalInitialSyncState reconciledState =
                        completePhaseAndReconcile(currentPhase);

                if (isQuotaOrExecutionLimitReached(result)) {
                    festivalInitialSyncStateService.waitForDailyQuota(
                            "당일 TourAPI 호출 한도 또는 실행 호출 한도에 도달해 "
                                    + "다음 자동 실행에서 초기 적재를 이어서 처리합니다."
                    );

                    log.info(
                            "축제·체험 초기 적재가 호출 한도에 도달해 대기 상태로 전환되었습니다. "
                                    + "phase={}, skippedDetailCount={}, tourApiCallCount={}",
                            currentPhase,
                            result.getSkippedDetailCount(),
                            result.getTourApiCallCount()
                    );

                    return;
                }

                FestivalInitialSyncPhase nextPhase =
                        reconciledState.getPhase();

                if (nextPhase == FestivalInitialSyncPhase.READY) {
                    logCompleted(result, currentPhase, nextPhase);
                    return;
                }

                if (nextPhase == currentPhase) {
                    if (result.getFailedRequestCount() > 0) {
                        festivalInitialSyncStateService.waitForTourApiError(
                                "일부 TourAPI 요청이 실패해 재시도 대상만 다음 실행에서 다시 처리합니다."
                        );
                    }

                    logPaused(result, currentPhase);
                    return;
                }

                festivalInitialSyncStateService.startPhase(nextPhase);

                log.info(
                        "축제·체험 초기 적재 단계를 이어서 실행합니다. currentPhase={}, nextPhase={}",
                        currentPhase,
                        nextPhase
                );

                currentPhase = nextPhase;
            }
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
        return switch (phase) {
            case NOT_STARTED, LIST_SYNCING ->
                    festivalSyncService.syncInitialLists(
                            DEFAULT_SIZE,
                            DEFAULT_MAX_PAGES,
                            DEFAULT_MAX_API_CALLS
                    );
            case DETAIL_SYNCING ->
                    festivalSyncService.syncInitialDetails(
                            DEFAULT_DETAIL_LIMIT,
                            DEFAULT_MAX_API_CALLS
                    );
            case IMAGE_SYNCING ->
                    festivalSyncService.syncInitialImages(
                            DEFAULT_DETAIL_LIMIT,
                            DEFAULT_MAX_API_CALLS
                    );
            case READY -> throw new IllegalStateException(
                    "READY 상태에서는 초기 적재 Worker를 실행할 수 없습니다."
            );
        };
    }

    private FestivalInitialSyncState completePhaseAndReconcile(
            FestivalInitialSyncPhase phase
    ) {
        if (phase == FestivalInitialSyncPhase.LIST_SYNCING
                || phase == FestivalInitialSyncPhase.NOT_STARTED) {
            return festivalInitialSyncStateService
                    .completeListPhaseAndReconcile();
        }

        return festivalInitialSyncStateService
                .completeCurrentPhaseAndReconcile();
    }

    private void handleIncompleteListResult(
            FestivalSyncResultResponse result
    ) {
        if (isQuotaOrExecutionLimitReached(result)) {
            festivalInitialSyncStateService.waitForDailyQuota(
                    "목록 수집이 일일 TourAPI 호출 한도 또는 실행 호출 한도에 도달해 "
                            + "다음 자동 실행에서 이어집니다."
            );

            return;
        }

        festivalInitialSyncStateService.waitForTourApiError(
                "목록 수집이 완전하게 끝나지 않아 기존 콘텐츠 비활성화와 "
                        + "다음 초기 적재 단계를 보류합니다."
        );
    }

    private boolean isQuotaOrExecutionLimitReached(
            FestivalSyncResultResponse result
    ) {
        return result.isDailyQuotaExceeded()
                || result.getSkippedDetailCount() > 0;
    }

    private void logCompleted(
            FestivalSyncResultResponse result,
            FestivalInitialSyncPhase currentPhase,
            FestivalInitialSyncPhase nextPhase
    ) {
        log.info(
                """
                축제·체험 초기 적재 완료
                - currentPhase={}
                - nextPhase={}
                - festivalSavedCount={}
                - experienceSavedCount={}
                - detailSyncedCount={}
                - failedRequestCount={}
                - tourApiCallCount={}
                """,
                currentPhase,
                nextPhase,
                result.getFestivalSavedCount(),
                result.getExperienceSavedCount(),
                result.getDetailSyncedCount(),
                result.getFailedRequestCount(),
                result.getTourApiCallCount()
        );
    }

    private void logPaused(
            FestivalSyncResultResponse result,
            FestivalInitialSyncPhase phase
    ) {
        log.info(
                """
                축제·체험 초기 적재 단계 처리 보류
                - phase={}
                - detailSyncedCount={}
                - skippedDetailCount={}
                - failedRequestCount={}
                - tourApiCallCount={}
                """,
                phase,
                result.getDetailSyncedCount(),
                result.getSkippedDetailCount(),
                result.getFailedRequestCount(),
                result.getTourApiCallCount()
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
