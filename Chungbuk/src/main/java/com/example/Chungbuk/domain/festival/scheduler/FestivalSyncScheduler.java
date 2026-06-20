package com.example.Chungbuk.domain.festival.scheduler;

import com.example.Chungbuk.domain.festival.constant.FestivalInitialSyncPhase;
import com.example.Chungbuk.domain.festival.dto.response.FestivalInitializationResponse;
import com.example.Chungbuk.domain.festival.dto.response.FestivalSyncResultResponse;
import com.example.Chungbuk.domain.festival.entity.FestivalInitialSyncState;
import com.example.Chungbuk.domain.festival.service.FestivalInitialSyncService;
import com.example.Chungbuk.domain.festival.service.FestivalInitialSyncStateService;
import com.example.Chungbuk.domain.festival.service.FestivalSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalSyncScheduler {

    private final FestivalSyncService festivalSyncService;

    private final FestivalInitialSyncStateService
            festivalInitialSyncStateService;

    private final FestivalInitialSyncService festivalInitialSyncService;

    @Scheduled(
            cron = "${festival.sync.scheduler.cron:0 0 3 * * *}",
            zone = "${festival.sync.scheduler.zone:Asia/Seoul}"
    )
    public void refreshFestivalContentsDaily() {
        try {
            FestivalInitialSyncState initialSyncState =
                    festivalInitialSyncStateService.getCurrentState();

            if (initialSyncState.getPhase()
                    != FestivalInitialSyncPhase.READY) {
                FestivalInitializationResponse response =
                        festivalInitialSyncService.ensureInitialized();

                log.info(
                        """
                        축제·체험 새벽 초기 적재 이어받기 요청
                        - phase={}
                        - executionStatus={}
                        - pauseReason={}
                        - initializationStarted={}
                        - initializationAlreadyRunning={}
                        - message={}
                        """,
                        response.getInitialSyncPhase(),
                        response.getInitialSyncExecutionStatus(),
                        response.getInitialSyncPauseReason(),
                        response.isInitializationStarted(),
                        response.isInitializationAlreadyRunning(),
                        response.getMessage()
                );

                return;
            }

            FestivalSyncResultResponse result =
                    festivalSyncService.refreshFestivalContents();

            log.info(
                    """
                    축제·체험 새벽 자동 refresh 완료
                    - festivalSavedCount={}
                    - experienceSavedCount={}
                    - detailSyncedCount={}
                    - deactivatedCount={}
                    - skippedDetailCount={}
                    - detailEmptyCount={}
                    - failedRequestCount={}
                    - tourApiCallCount={}
                    - message={}
                    """,
                    result.getFestivalSavedCount(),
                    result.getExperienceSavedCount(),
                    result.getDetailSyncedCount(),
                    result.getDeactivatedCount(),
                    result.getSkippedDetailCount(),
                    result.getDetailEmptyCount(),
                    result.getFailedRequestCount(),
                    result.getTourApiCallCount(),
                    result.getMessage()
            );
        } catch (Exception e) {
            log.error("축제·체험 새벽 자동 동기화 중 오류가 발생했습니다.", e);
        }
    }
}
